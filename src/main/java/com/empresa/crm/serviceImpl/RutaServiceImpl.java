package com.empresa.crm.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.empresa.crm.dto.RutaDiaItemDTO;
import com.empresa.crm.dto.RutaDiaRequestDTO;
import com.empresa.crm.entities.Cliente;
import com.empresa.crm.entities.ClienteProducto;
import com.empresa.crm.entities.Producto;
import com.empresa.crm.entities.Ruta;
import com.empresa.crm.entities.RutaLinea;
import com.empresa.crm.entities.Trabajo;
import com.empresa.crm.repositories.ClienteProductoRepository;
import com.empresa.crm.repositories.ClienteRepository;
import com.empresa.crm.repositories.ProductoRepository;
import com.empresa.crm.repositories.RutaRepository;
import com.empresa.crm.repositories.TrabajoRepository;
import com.empresa.crm.scheduler.RutaScheduler;
import com.empresa.crm.services.ClienteProductoService;
import com.empresa.crm.services.RutaService;
import com.empresa.crm.tenant.TenantContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RutaServiceImpl implements RutaService {

	private final RutaRepository rutaRepository;
	private final ClienteRepository clienteRepository;
	private final ClienteProductoRepository clienteProductoRepository;
	private final TrabajoRepository trabajoRepository;
	private final ProductoRepository productoRepository;
	private final RutaScheduler rutaScheduler;

	private final ClienteProductoService clienteProductoService;

	@Override
	public List<Ruta> findAll() {
		return rutaRepository.findByEmpresa(TenantContext.get());
	}

	@Override
	public Ruta findById(Long id) {
		return rutaRepository.findById(id).orElse(null);
	}

	@Override
	public Ruta save(Ruta ruta) {
		if (ruta.getEmpresa() == null || ruta.getEmpresa().isBlank()) {
			throw new IllegalArgumentException("Empresa obligatoria (ARGASA / ELECTROLUGA)");
		}

		if (ruta.getCliente() == null || ruta.getCliente().getId() == null) {
			throw new IllegalArgumentException("Cliente obligatorio en la ruta");
		}

		Cliente c = clienteRepository.findByIdAndEmpresa(ruta.getCliente().getId(), ruta.getEmpresa()).orElseThrow(
				() -> new IllegalArgumentException("Cliente no encontrado en empresa " + ruta.getEmpresa()));

		ruta.setCliente(c);

		if (ruta.getDestino() == null || ruta.getDestino().isBlank()) {
			ruta.setDestino(buildDireccionEntregaCompleta(c));
		}

		Ruta guardada = rutaRepository.save(ruta);

		LocalDate hoy = LocalDate.now();
		LocalTime ahora = LocalTime.now();
		boolean rutaEsHoy = ruta.getFecha() != null && ruta.getFecha().isEqual(hoy);

		if (rutaEsHoy && ahora.isAfter(LocalTime.of(8, 0))) {
			rutaScheduler.enviarActualizacionHoy(ruta.getNombreTransportista());
		}

		return guardada;
	}

	@Override
	public void deleteById(Long id) {
		rutaRepository.deleteById(id);
	}

	@Override
	public List<Ruta> findByEstado(String estado) {
		return rutaRepository.findByEmpresaAndEstado(TenantContext.get(), estado);
	}

	@Override
	public List<Ruta> findByNombreTransportista(String nombre) {
		return rutaRepository.findByEmpresaAndNombreTransportistaContainingIgnoreCase(TenantContext.get(), nombre);
	}

	@Override
	public List<Ruta> findByFecha(LocalDate fecha) {
		return rutaRepository.findByEmpresaAndFecha(TenantContext.get(), fecha);
	}

	@Override
	@Transactional
	public Ruta cerrarRuta(Long id) {

		final String empresa = TenantContext.get();

		Ruta ruta = rutaRepository.findWithLineasByIdAndEmpresa(id, empresa)
				.orElseThrow(() -> new RuntimeException("Ruta no encontrada o no pertenece a la empresa"));

		if ("cerrada".equalsIgnoreCase(ruta.getEstado())) {
			return ruta;
		}

		ruta.setEstado("cerrada");

		marcarEntregasClienteProductoYTrabajos(ruta, empresa);

		return rutaRepository.save(ruta);
	}

	private void marcarEntregasClienteProductoYTrabajos(Ruta ruta, String empresa) {

		if (ruta.getCliente() == null || ruta.getCliente().getId() == null)
			return;
		if (ruta.getLineas() == null || ruta.getLineas().isEmpty())
			return;

		Long clienteId = ruta.getCliente().getId();
		LocalDateTime ahora = LocalDateTime.now();

		for (RutaLinea l : ruta.getLineas()) {

			if (l == null || l.getProducto() == null || l.getProducto().getId() == null)
				continue;

			Long productoId = l.getProducto().getId();
			int cantidad = safeInt(l.getCantidad());
			if (cantidad <= 0)
				continue;

			l.setEstado("ENTREGADO");
			l.setFechaEntrega(ahora);

			ClienteProducto cp = clienteProductoRepository
					.findByEmpresaAndClienteIdAndProductoId(empresa, clienteId, productoId).orElse(null);

			if (cp != null) {

				int total = safeInt(cp.getCantidadTotal());
				int entregada = safeInt(cp.getCantidadEntregada());

				int nuevaEntregada = entregada + cantidad;

				cp.setCantidadEntregada(nuevaEntregada);
				cp.setFechaEntrega(ahora);

				boolean entregadoCompleto = nuevaEntregada >= total;

				cp.setEntregado(entregadoCompleto);
				cp.setEstado(entregadoCompleto ? "ENTREGADO" : "PARCIAL");

				clienteProductoRepository.save(cp);
			}

			List<Trabajo> trabajos = trabajoRepository.findByEmpresaAndClienteIdAndProductoId(empresa, clienteId,
					productoId);

			marcarTrabajosEntregadosParcialmente(trabajos, cantidad, ahora);

			// No se descuenta stock al cerrar la ruta.
			// El stock ya fue descontado en el momento de la venta/asignación al cliente.
		}
	}

	private void marcarTrabajosEntregadosParcialmente(List<Trabajo> trabajos, int cantidadEntregada,
			LocalDateTime ahora) {

		if (trabajos == null || trabajos.isEmpty() || cantidadEntregada <= 0) {
			return;
		}

		int pendientesPorAsignar = cantidadEntregada;
		List<Trabajo> nuevosTrabajosEntregados = new ArrayList<>();

		for (Trabajo t : trabajos) {
			if (pendientesPorAsignar <= 0) {
				break;
			}

			if (t == null || t.isEntregado()) {
				continue;
			}

			int unidadesTrabajo = safeInt(t.getUnidades());
			if (unidadesTrabajo <= 0) {
				unidadesTrabajo = 1;
			}

			// Caso 1: el trabajo entero queda entregado
			if (unidadesTrabajo <= pendientesPorAsignar) {
				t.setEntregado(true);
				t.setFechaEntrega(ahora);
				pendientesPorAsignar -= unidadesTrabajo;
				continue;
			}

			// Caso 2: entrega parcial -> se divide el trabajo en dos
			Trabajo entregado = new Trabajo();
			entregado.setCliente(t.getCliente());
			entregado.setProductoId(t.getProductoId());
			entregado.setDescripcion(t.getDescripcion());
			entregado.setUnidades(pendientesPorAsignar);
			entregado.setDescuento(t.getDescuento());
			entregado.setImportePagado(t.getImportePagado());
			entregado.setEmpresa(t.getEmpresa());
			entregado.setEntregado(true);
			entregado.setFechaEntrega(ahora);

			// El trabajo original se queda pendiente con las unidades restantes
			t.setUnidades(unidadesTrabajo - pendientesPorAsignar);

			nuevosTrabajosEntregados.add(entregado);
			pendientesPorAsignar = 0;
		}

		trabajoRepository.saveAll(trabajos);

		if (!nuevosTrabajosEntregados.isEmpty()) {
			trabajoRepository.saveAll(nuevosTrabajosEntregados);
		}
	}

	@Override
	@Transactional
	public List<Ruta> crearRutasDeUnDia(RutaDiaRequestDTO request) {

		final String empresa = (request.getEmpresa() != null && !request.getEmpresa().isBlank())
				? request.getEmpresa().trim()
				: TenantContext.get();

		if (empresa == null || empresa.isBlank()) {
			throw new IllegalArgumentException("Empresa obligatoria (ARGASA / ELECTROLUGA)");
		}

		final String tenantPrevio = TenantContext.get();
		TenantContext.set(empresa);

		try {
			return crearRutasDeUnDiaCore(request, empresa);
		} finally {
			if (tenantPrevio == null || tenantPrevio.isBlank()) {
				try {
					TenantContext.clear();
				} catch (Exception ex) {
					TenantContext.set(tenantPrevio);
				}
			} else {
				TenantContext.set(tenantPrevio);
			}
		}
	}

	private List<Ruta> crearRutasDeUnDiaCore(RutaDiaRequestDTO request, String empresa) {

		if (request.getFecha() == null || request.getFecha().isBlank()) {
			throw new IllegalArgumentException("Fecha obligatoria");
		}
		final LocalDate fechaRuta = parseFecha(request.getFecha());

		if (request.getNombreTransportista() == null || request.getNombreTransportista().isBlank()) {
			throw new IllegalArgumentException("Nombre de transportista obligatorio");
		}

		if (request.getRutas() == null || request.getRutas().isEmpty()) {
			throw new IllegalArgumentException("Debe haber al menos una ruta");
		}

		final String estadoGlobal = (request.getEstado() != null && !request.getEstado().isBlank())
				? request.getEstado().trim()
				: "pendiente";

		List<Ruta> aGuardar = new ArrayList<>();

		for (RutaDiaItemDTO item : request.getRutas()) {
			if (item == null)
				continue;

			if (item.getClienteId() == null) {
				throw new IllegalArgumentException("clienteId obligatorio en cada fila");
			}

			Cliente c = clienteRepository.findByIdAndEmpresa(item.getClienteId(), empresa)
					.orElseThrow(() -> new IllegalArgumentException(
							"Cliente no encontrado: " + item.getClienteId() + " en empresa " + empresa));

			Ruta r = new Ruta();
			r.setEmpresa(empresa);
			r.setFecha(fechaRuta);
			r.setNombreTransportista(request.getNombreTransportista());
			r.setEmailTransportista(request.getEmailTransportista());

			String estadoFinal = (item.getEstado() != null && !item.getEstado().isBlank()) ? item.getEstado().trim()
					: estadoGlobal;
			r.setEstado(estadoFinal);

			r.setCliente(c);
			r.setTarea(item.getTarea());
			r.setObservaciones(item.getObservaciones());

			if (r.getDestino() == null || r.getDestino().isBlank()) {
				r.setDestino(buildDireccionEntregaCompleta(c));
			}

			if (item.getProductos() != null && !item.getProductos().isEmpty()) {
				if (r.getLineas() == null)
					r.setLineas(new ArrayList<>());

				for (var p : item.getProductos()) {

					if (p == null || p.getProducto() == null)
						continue;

					int cantSolicitada = safeInt(p.getCantidad());
					if (cantSolicitada <= 0)
						continue;

					Long productoId = p.getProducto();

					Producto prod = productoRepository.findByIdAndEmpresa(productoId, empresa)
							.orElseThrow(() -> new IllegalArgumentException(
									"Producto no encontrado: " + productoId + " en empresa " + empresa));

					ClienteProducto cp = clienteProductoService.ensureAsignacion(c.getId(), prod.getId(),
							cantSolicitada);

					if (isEntregadoCompleto(cp)) {
						throw new IllegalArgumentException(
								"El producto " + productoId + " ya está ENTREGADO para el cliente "
										+ item.getClienteId() + " en empresa " + empresa);
					}

					int totalAsignado = safeInt(cp.getCantidadTotal());
					int entregado = safeInt(cp.getCantidadEntregada());

					if (totalAsignado <= 0) {
						throw new IllegalArgumentException(
								"No se puede validar cantidad: cantidadTotal no definida para cliente "
										+ item.getClienteId() + " y producto " + productoId + " en empresa " + empresa);
					}

					int reservadoCliente = safeInt(rutaRepository.sumReservadoClienteProductoAbierto(empresa,
							item.getClienteId(), productoId));

					int pendienteReal = totalAsignado - entregado - reservadoCliente;
					if (pendienteReal <= 0) {
						throw new IllegalArgumentException("Sin pendiente: el producto " + productoId
								+ " no tiene pendiente REAL para el cliente " + item.getClienteId() + " en empresa "
								+ empresa + " (pendiente real=" + pendienteReal + ")");
					}

					if (cantSolicitada > pendienteReal) {
						throw new IllegalArgumentException("Cantidad solicitada (" + cantSolicitada
								+ ") supera la pendiente REAL (" + pendienteReal + ") para cliente "
								+ item.getClienteId() + " y producto " + productoId + " en empresa " + empresa);
					}

					// NO validamos contra el stock actual del producto aquí.
					// La ruta debe poder generarse si el cliente tiene pendiente real,
					// aunque el stock actual sea 0 o negativo, porque ese producto ya fue vendido.

					RutaLinea linea = new RutaLinea();
					linea.setRuta(r);
					linea.setProducto(prod);
					linea.setCantidad(cantSolicitada);
					linea.setEstado("PENDIENTE");
					r.getLineas().add(linea);
				}
			}

			aGuardar.add(r);
		}

		List<Ruta> guardadas = rutaRepository.saveAll(aGuardar);

		LocalDate hoy = LocalDate.now();
		LocalTime ahora = LocalTime.now();
		if (fechaRuta.isEqual(hoy) && ahora.isAfter(LocalTime.of(8, 0))) {
			rutaScheduler.enviarActualizacionHoy(request.getNombreTransportista());
		}

		return guardadas;
	}

	@Override
	@Transactional
	public List<Ruta> crearRutasDeUnDiaTx(RutaDiaRequestDTO request, String empresa) {
		if (empresa == null || empresa.isBlank()) {
			empresa = TenantContext.get();
		}
		if (empresa == null || empresa.isBlank()) {
			throw new IllegalArgumentException("Empresa obligatoria (ARGASA / ELECTROLUGA)");
		}

		final String tenantPrevio = TenantContext.get();
		TenantContext.set(empresa);

		try {
			return crearRutasDeUnDiaCore(request, empresa);
		} finally {
			if (tenantPrevio == null || tenantPrevio.isBlank()) {
				try {
					TenantContext.clear();
				} catch (Exception ex) {
					TenantContext.set(tenantPrevio);
				}
			} else {
				TenantContext.set(tenantPrevio);
			}
		}
	}

	private LocalDate parseFecha(String fecha) {
		String f = fecha.trim();

		if (f.matches("\\d{4}-\\d{2}-\\d{2}")) {
			return LocalDate.parse(f);
		}

		try {
			java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
			return LocalDate.parse(f, fmt);
		} catch (Exception e) {
			throw new IllegalArgumentException(
					"Formato de fecha inválido: " + fecha + " (usa dd/MM/yyyy o yyyy-MM-dd)");
		}
	}

	private boolean isEntregadoCompleto(ClienteProducto cp) {
		if (cp == null)
			return false;

		String estado = cp.getEstado();
		if (estado != null && estado.equalsIgnoreCase("ENTREGADO"))
			return true;

		int total = safeInt(cp.getCantidadTotal());
		int entregada = safeInt(cp.getCantidadEntregada());
		return total > 0 && entregada >= total;
	}

	private int safeInt(Integer n) {
		return (n == null) ? 0 : n.intValue();
	}

	/**
	 * Usa dirección de entrega si existe; si no, usa la de facturación.
	 */
	private String buildDireccionEntregaCompleta(Cliente c) {
		String dir = safe(notBlankOrElse(c.getDireccionEntrega(), c.getDireccion()));
		String cp = safe(notBlankOrElse(c.getCodigoPostalEntrega(), c.getCodigoPostal()));
		String pob = safe(notBlankOrElse(c.getPoblacionEntrega(), c.getPoblacion()));
		String prov = safe(notBlankOrElse(c.getProvinciaEntrega(), c.getProvincia()));

		String medio = (cp + " " + pob).trim();
		String fin = prov.isBlank() ? "" : (" (" + prov + ")");

		String res = (dir + (medio.isBlank() ? "" : (", " + medio)) + fin).trim();
		return res.isBlank() ? "-" : res;
	}

	private String notBlankOrElse(String preferred, String fallback) {
		String p = safe(preferred);
		return !p.isBlank() ? p : safe(fallback);
	}

	private String safe(String s) {
		return s == null ? "" : s.trim();
	}
}
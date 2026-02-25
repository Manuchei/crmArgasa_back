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

		Cliente c = clienteRepository.findById(ruta.getCliente().getId())
				.orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));

		ruta.setCliente(c);

		if (ruta.getDestino() == null || ruta.getDestino().isBlank()) {
			ruta.setDestino(buildDireccionCompleta(c));
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

	/**
	 * ✅ Cierra la ruta y marca entregas en cliente_producto + trabajos
	 */
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

		// ✅ marcar entregas (cliente_producto y trabajos)
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
			int cantidadEntregadaEnRuta = safeInt(l.getCantidad());
			if (cantidadEntregadaEnRuta <= 0)
				continue;

			ClienteProducto cp = clienteProductoRepository.findByClienteIdAndProductoId(clienteId, productoId)
					.orElse(null);
			if (cp == null)
				continue;

			// ✅ si ya está entregado completo, no vuelvas a sumar
			if (isEntregadoCompleto(cp))
				continue;

			int total = safeInt(cp.getCantidadTotal());
			int entregada = safeInt(cp.getCantidadEntregada());
			if (total <= 0)
				continue;

			int pendiente = total - entregada;
			if (pendiente <= 0)
				continue;

			// ✅ cap: nunca sumar más de lo pendiente
			int aSumar = Math.min(cantidadEntregadaEnRuta, pendiente);

			int nuevaEntregada = entregada + aSumar;
			cp.setCantidadEntregada(nuevaEntregada);
			cp.setFechaEntrega(ahora);

			boolean entregadoCompleto = nuevaEntregada >= total;
			cp.setEntregado(entregadoCompleto);
			cp.setEstado(entregadoCompleto ? "ENTREGADO" : "PARCIAL");

			clienteProductoRepository.save(cp);

			if (entregadoCompleto) {
				List<Trabajo> trabajos = trabajoRepository.findByEmpresaAndClienteIdAndProductoId(empresa, clienteId,
						productoId);
				for (Trabajo t : trabajos) {
					t.setEntregado(true);
					t.setFechaEntrega(ahora);
				}
				trabajoRepository.saveAll(trabajos);
			}
		}
	}

	/**
	 * ✅ Crea rutas del día con BLOQUEO REAL: - Pendiente REAL = total - entregado -
	 * reservado(en rutas NO cerradas de esa fecha) - Stock REAL = stock -
	 * reservado(total en rutas NO cerradas de esa fecha)
	 *
	 * ✅ IMPORTANTE: se fuerza el TenantContext al "empresa" del request para evitar
	 * inconsistencias (caso: UI muestra asignación, pero al guardar dice "no tiene
	 * asignado").
	 */
	@Override
	@Transactional
	public List<Ruta> crearRutasDeUnDia(RutaDiaRequestDTO request) {

		final String empresa = (request.getEmpresa() != null && !request.getEmpresa().isBlank())
				? request.getEmpresa().trim()
				: TenantContext.get();

		if (empresa == null || empresa.isBlank()) {
			throw new IllegalArgumentException("Empresa obligatoria (ARGASA / ELECTROLUGA)");
		}

		// ✅ CLAVE: asegurar tenant correcto en este hilo/request
		final String tenantPrevio = TenantContext.get();
		TenantContext.set(empresa);

		try {

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

				Cliente c = clienteRepository.findById(item.getClienteId()).orElseThrow(
						() -> new IllegalArgumentException("Cliente no encontrado: " + item.getClienteId()));

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
					r.setDestino(buildDireccionCompleta(c));
				}

				// ✅ LINEAS (productos)
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

						// ✅ Producto
						Producto prod = productoRepository.findById(productoId).orElseThrow(
								() -> new IllegalArgumentException("Producto no encontrado: " + productoId));

						// ✅ ClienteProducto (asignación)
						ClienteProducto cp = clienteProductoRepository
								.findByClienteIdAndProductoId(item.getClienteId(), productoId)
								.orElseThrow(() -> new IllegalArgumentException("El cliente " + item.getClienteId()
										+ " no tiene asignado el producto " + productoId));

						// ✅ si ya ENTREGADO completo -> no permitir
						if (isEntregadoCompleto(cp)) {
							throw new IllegalArgumentException("El producto " + productoId
									+ " ya está ENTREGADO para el cliente " + item.getClienteId());
						}

						int totalAsignado = safeInt(cp.getCantidadTotal());
						int entregado = safeInt(cp.getCantidadEntregada());

						if (totalAsignado <= 0) {
							throw new IllegalArgumentException(
									"No se puede validar cantidad: cantidadTotal no definida para cliente "
											+ item.getClienteId() + " y producto " + productoId);
						}

						// ✅ RESERVADO (cliente+producto en rutas NO cerradas esa fecha)
						int reservadoCliente = safeInt(rutaRepository.sumReservadoClienteProductoFecha(empresa,
								fechaRuta, item.getClienteId(), productoId));

						int pendienteReal = totalAsignado - entregado - reservadoCliente;
						if (pendienteReal <= 0) {
							throw new IllegalArgumentException("Sin pendiente: el producto " + productoId
									+ " no tiene pendiente REAL para el cliente " + item.getClienteId()
									+ " (pendiente real=" + pendienteReal + ")");
						}

						if (cantSolicitada > pendienteReal) {
							throw new IllegalArgumentException("Cantidad solicitada (" + cantSolicitada
									+ ") supera la pendiente REAL (" + pendienteReal + ") para cliente "
									+ item.getClienteId() + " y producto " + productoId);
						}

						// ✅ STOCK REAL: stock - reservado total del producto en rutas NO cerradas esa
						// fecha
						int stock = safeInt(prod.getStock());
						int reservadoProducto = safeInt(
								rutaRepository.sumReservadoProductoFecha(empresa, fechaRuta, productoId));

						int stockReal = stock - reservadoProducto;
						if (stockReal <= 0) {
							throw new IllegalArgumentException("Sin stock REAL para producto " + productoId + " (stock="
									+ stock + ", reservado=" + reservadoProducto + ")");
						}

						if (cantSolicitada > stockReal) {
							throw new IllegalArgumentException("Cantidad solicitada (" + cantSolicitada
									+ ") supera el stock REAL (" + stockReal + ") para producto " + productoId);
						}

						// ✅ crear línea
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

		} finally {
			// ✅ restaurar tenant anterior (evita que el hilo se quede “pegado” a una
			// empresa)
			if (tenantPrevio == null || tenantPrevio.isBlank()) {
				// si tienes clear() úsalo, si no, deja set("") o set(null) según tu
				// implementación
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

	// ===================== HELPERS =====================

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

	private String buildDireccionCompleta(Cliente c) {
		String dir = safe(c.getDireccion());
		String cp = safe(c.getCodigoPostal());
		String pob = safe(c.getPoblacion());
		String prov = safe(c.getProvincia());

		String medio = (cp + " " + pob).trim();
		String fin = prov.isBlank() ? "" : (" (" + prov + ")");

		String res = (dir + (medio.isBlank() ? "" : (", " + medio)) + fin).trim();
		return res.isBlank() ? "-" : res;
	}

	private String safe(String s) {
		return s == null ? "" : s.trim();
	}
}
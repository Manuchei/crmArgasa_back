package com.empresa.crm.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.empresa.crm.dto.RutaDiaItemDTO;
import com.empresa.crm.dto.RutaDiaRequestDTO;
import com.empresa.crm.entities.Cliente;
import com.empresa.crm.entities.ClienteProducto;
import com.empresa.crm.entities.Ruta;
import com.empresa.crm.entities.RutaLinea;
import com.empresa.crm.repositories.ClienteProductoRepository;
import com.empresa.crm.repositories.ClienteRepository;
import com.empresa.crm.repositories.RutaRepository;
import com.empresa.crm.scheduler.RutaScheduler;
import com.empresa.crm.services.RutaService;
import com.empresa.crm.tenant.TenantContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RutaServiceImpl implements RutaService {

	private final RutaRepository rutaRepository;
	private final ClienteRepository clienteRepository;
	private final ClienteProductoRepository clienteProductoRepository; // ✅ NUEVO
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
		boolean despuesDeLas8 = ahora.isAfter(LocalTime.of(8, 0));
		if (rutaEsHoy && despuesDeLas8) {
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
	 * ✅ Cierra la ruta y marca entregas en cliente_producto
	 */
	@Override
	@Transactional
	public Ruta cerrarRuta(Long id) {
		Ruta ruta = findById(id);

		if (ruta == null)
			return null;

		if (!"cerrada".equalsIgnoreCase(ruta.getEstado())) {
			ruta.setEstado("cerrada");

			// ✅ marcar entregas (si hay líneas)
			marcarEntregasClienteProducto(ruta);

			rutaRepository.save(ruta);

			LocalDate hoy = LocalDate.now();
			LocalTime ahora = LocalTime.now();

			if (ruta.getFecha() != null && ruta.getFecha().isEqual(hoy) && ahora.isAfter(LocalTime.of(8, 0))) {
				rutaScheduler.enviarActualizacionHoy(ruta.getNombreTransportista());
			}
		}

		return ruta;
	}

	private void marcarEntregasClienteProducto(Ruta ruta) {
		if (ruta.getCliente() == null || ruta.getCliente().getId() == null)
			return;

		Long clienteId = ruta.getCliente().getId();
		LocalDateTime ahora = LocalDateTime.now();

		if (ruta.getLineas() == null || ruta.getLineas().isEmpty())
			return;

		for (RutaLinea l : ruta.getLineas()) {
			if (l == null || l.getProducto() == null || l.getProducto().getId() == null)
				continue;

			Long productoId = l.getProducto().getId();
			int cantidadEntregadaEnRuta = (l.getCantidad() == null) ? 0 : l.getCantidad();

			if (cantidadEntregadaEnRuta <= 0)
				continue;

			ClienteProducto cp = clienteProductoRepository.findByClienteIdAndProductoId(clienteId, productoId)
					.orElse(null);

			if (cp == null)
				continue;

			// ✅ Si ya está entregado, no hacemos nada
			if (cp.isEntregado()) {
				continue;
			}

			int total = (cp.getCantidadTotal() == null) ? 0 : cp.getCantidadTotal();
			int entregada = (cp.getCantidadEntregada() == null) ? 0 : cp.getCantidadEntregada();

			int nuevaEntregada = entregada + cantidadEntregadaEnRuta;
			if (nuevaEntregada > total)
				nuevaEntregada = total; // ✅ nunca pasar del total

			cp.setCantidadEntregada(nuevaEntregada);
			cp.setFechaEntrega(ahora);

			if (total > 0 && nuevaEntregada >= total) {
				cp.setEntregado(true);
				cp.setEstado("ENTREGADO");
			} else {
				cp.setEntregado(false);
				cp.setEstado("PARCIAL"); // o "PENDIENTE" si prefieres
			}

			clienteProductoRepository.save(cp);
		}
	}

	@Override
	@Transactional
	public List<Ruta> crearRutasDeUnDia(RutaDiaRequestDTO request) {

		DateTimeFormatter iso = DateTimeFormatter.ISO_LOCAL_DATE;
		DateTimeFormatter es = DateTimeFormatter.ofPattern("dd/MM/yyyy");

		String f = request.getFecha();
		LocalDate fecha;
		try {
			fecha = LocalDate.parse(f, iso);
		} catch (Exception e) {
			fecha = LocalDate.parse(f, es);
		}

		String estadoBase = (request.getEstado() == null || request.getEstado().isBlank()) ? "pendiente"
				: request.getEstado();

		if (request.getRutas() == null || request.getRutas().isEmpty()) {
			return new ArrayList<>();
		}

		String empresaBase = (request.getEmpresa() == null) ? null : request.getEmpresa().trim();

		List<Ruta> nuevas = new ArrayList<>();

		for (RutaDiaItemDTO item : request.getRutas()) {

			if (item.getClienteId() == null) {
				throw new IllegalArgumentException("clienteId obligatorio en cada item de rutas");
			}

			String empresaFinal = (item.getEmpresa() != null && !item.getEmpresa().isBlank()) ? item.getEmpresa().trim()
					: empresaBase;

			if (empresaFinal == null || empresaFinal.isBlank()) {
				throw new IllegalArgumentException(
						"Empresa obligatoria (ARGASA / ELECTROLUGA) en request.empresa o item.empresa");
			}

			Cliente c = clienteRepository.findById(item.getClienteId())
					.orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado: " + item.getClienteId()));

			Ruta r = new Ruta();
			r.setFecha(fecha);
			r.setNombreTransportista(request.getNombreTransportista());
			r.setEmailTransportista(request.getEmailTransportista());

			r.setCliente(c);
			r.setDestino(buildDireccionCompleta(c));

			r.setOrigen("");

			r.setTarea(item.getTarea());
			r.setObservaciones(item.getObservaciones());

			String estadoFinal = (item.getEstado() == null || item.getEstado().isBlank()) ? estadoBase
					: item.getEstado();

			r.setEstado(estadoFinal);
			r.setEmpresa(empresaFinal);

			nuevas.add(r);
		}

		List<Ruta> guardadas = rutaRepository.saveAll(nuevas);

		LocalDate hoy = LocalDate.now();
		LocalTime ahora = LocalTime.now();
		if (fecha.isEqual(hoy) && ahora.isAfter(LocalTime.of(8, 0))) {
			rutaScheduler.enviarActualizacionHoy(request.getNombreTransportista());
		}

		return guardadas;
	}

	// ---------------- helpers ----------------
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

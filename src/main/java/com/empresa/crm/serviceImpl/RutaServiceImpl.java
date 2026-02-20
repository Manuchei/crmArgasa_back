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
import com.empresa.crm.entities.Ruta;
import com.empresa.crm.entities.RutaLinea;
import com.empresa.crm.entities.Trabajo;
import com.empresa.crm.repositories.ClienteProductoRepository;
import com.empresa.crm.repositories.ClienteRepository;
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
	private final TrabajoRepository trabajoRepository; // ✅ NUEVO
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

		// ✅ asegura persistencia del estado también
		return rutaRepository.save(ruta);
	}

	private void marcarEntregasClienteProductoYTrabajos(Ruta ruta, String empresa) {

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

			int total = (cp.getCantidadTotal() == null) ? 0 : cp.getCantidadTotal();
			int entregada = (cp.getCantidadEntregada() == null) ? 0 : cp.getCantidadEntregada();

			int nuevaEntregada = entregada + cantidadEntregadaEnRuta;
			if (total > 0 && nuevaEntregada > total)
				nuevaEntregada = total;

			cp.setCantidadEntregada(nuevaEntregada);
			cp.setFechaEntrega(ahora);

			boolean entregadoCompleto = (total > 0 && nuevaEntregada >= total);
			cp.setEntregado(entregadoCompleto);
			cp.setEstado(entregadoCompleto ? "ENTREGADO" : "PARCIAL");

			clienteProductoRepository.save(cp);

			// ✅ SI el cliente_producto queda ENTREGADO, marcamos los trabajos de ese
			// producto como entregados
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

	@Override
	@Transactional
	public List<Ruta> crearRutasDeUnDia(RutaDiaRequestDTO request) {
		// (tu código igual que lo tenías, no lo toco aquí)
		return new ArrayList<>();
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
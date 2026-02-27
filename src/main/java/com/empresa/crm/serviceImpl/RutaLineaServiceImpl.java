package com.empresa.crm.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.empresa.crm.entities.ClienteProducto;
import com.empresa.crm.entities.RutaLinea;
import com.empresa.crm.repositories.ClienteProductoRepository;
import com.empresa.crm.repositories.RutaLineaRepository;
import com.empresa.crm.services.RutaLineaService;
import com.empresa.crm.tenant.TenantContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RutaLineaServiceImpl implements RutaLineaService {

	private final RutaLineaRepository rutaLineaRepo;
	private final ClienteProductoRepository clienteProductoRepo;

	@Override
	public List<RutaLinea> findByRuta(Long rutaId) {
		final String empresa = TenantContext.get();
		return rutaLineaRepo.findByRutaIdAndRutaEmpresa(rutaId, empresa);
	}

	@Override
	@Transactional
	public void confirmarEntrega(Long rutaLineaId) {

		final String empresa = TenantContext.get();

		// ✅ Cargar la línea validando empresa (ruta.empresa)
		RutaLinea linea = rutaLineaRepo.findByIdAndRutaEmpresa(rutaLineaId, empresa).orElseThrow(
				() -> new RuntimeException("Línea no encontrada o no pertenece a " + empresa + ": " + rutaLineaId));

		if ("ENTREGADO".equalsIgnoreCase(linea.getEstado())) {
			return;
		}

		linea.setEstado("ENTREGADO");
		linea.setFechaEntrega(LocalDateTime.now());
		rutaLineaRepo.save(linea);

		if (linea.getRuta() == null || linea.getRuta().getCliente() == null
				|| linea.getRuta().getCliente().getId() == null) {
			throw new IllegalArgumentException("La ruta no tiene cliente válido");
		}
		if (linea.getProducto() == null || linea.getProducto().getId() == null) {
			throw new IllegalArgumentException("La línea no tiene producto válido");
		}

		Long clienteId = linea.getRuta().getCliente().getId();
		Long productoId = linea.getProducto().getId();

		// ✅ ClienteProducto filtrado por empresa (aquí sí existe campo empresa)
		ClienteProducto cp = clienteProductoRepo.findByEmpresaAndClienteIdAndProductoId(empresa, clienteId, productoId)
				.orElseThrow(() -> new RuntimeException("ClienteProducto no existe para cliente " + clienteId
						+ " y producto " + productoId + " en " + empresa));

		// ✅ Sumar entrega (sin pasarse)
		int cantidadLinea = safeInt(linea.getCantidad());
		int entregada = safeInt(cp.getCantidadEntregada());
		int total = safeInt(cp.getCantidadTotal());

		int nuevaEntregada = entregada + Math.max(0, cantidadLinea);
		cp.setCantidadEntregada(nuevaEntregada);
		cp.setFechaEntrega(LocalDateTime.now());

		boolean entregadoCompleto = (total > 0) && (nuevaEntregada >= total);
		cp.setEntregado(entregadoCompleto);
		cp.setEstado(entregadoCompleto ? "ENTREGADO" : "PARCIAL");

		clienteProductoRepo.save(cp);
	}

	private int safeInt(Integer n) {
		return n == null ? 0 : n.intValue();
	}
}
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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RutaLineaServiceImpl implements RutaLineaService {

	private final RutaLineaRepository rutaLineaRepo;
	private final ClienteProductoRepository clienteProductoRepo;

	@Override
	public List<RutaLinea> findByRuta(Long rutaId) {
		return rutaLineaRepo.findByRutaId(rutaId);
	}

	@Override
	@Transactional
	public void confirmarEntrega(Long rutaLineaId) {

		RutaLinea linea = rutaLineaRepo.findById(rutaLineaId)
				.orElseThrow(() -> new RuntimeException("Línea no encontrada: " + rutaLineaId));

		if ("ENTREGADO".equalsIgnoreCase(linea.getEstado())) {
			return; // ya estaba confirmada
		}

		linea.setEstado("ENTREGADO");
		linea.setFechaEntrega(LocalDateTime.now());
		rutaLineaRepo.save(linea);

		Long clienteId = linea.getRuta().getCliente().getId();
		Long productoId = linea.getProducto().getId();

		ClienteProducto cp = clienteProductoRepo.findByClienteIdAndProductoId(clienteId, productoId)
				.orElseThrow(() -> new RuntimeException(
						"ClienteProducto no existe para cliente " + clienteId + " y producto " + productoId));

		// ✅ mínimo: marcar entregado
		cp.setEstado("ENTREGADO");
		cp.setFechaEntrega(LocalDateTime.now());

		clienteProductoRepo.save(cp);
	}
}

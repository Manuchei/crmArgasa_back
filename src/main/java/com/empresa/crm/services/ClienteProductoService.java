package com.empresa.crm.services;

import java.util.List;

import com.empresa.crm.dto.ClienteProductoAsignarDTO;
import com.empresa.crm.dto.ProductoPendienteDto;
import com.empresa.crm.entities.ClienteProducto;

public interface ClienteProductoService {

	ClienteProducto asignarProductoACliente(ClienteProductoAsignarDTO dto);

	List<ClienteProducto> listarPorCliente(Long clienteId);

	void eliminarAsignacion(Long clienteId, Long productoId);

	/**
	 * ✅ Para Rutas/Trabajos: - Si existe => devuelve - Si NO existe => crea la
	 * asignación automáticamente
	 */
	ClienteProducto ensureAsignacion(Long clienteId, Long productoId, Integer cantidadSolicitada);

	// ✅ NUEVO: SOLO productos con pendiente real
	List<ProductoPendienteDto> listarPendientesPorCliente(Long clienteId);
}
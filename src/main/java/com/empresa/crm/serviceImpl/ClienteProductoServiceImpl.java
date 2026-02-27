package com.empresa.crm.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.empresa.crm.dto.ClienteProductoAsignarDTO;
import com.empresa.crm.dto.ProductoPendienteDto;
import com.empresa.crm.entities.Cliente;
import com.empresa.crm.entities.ClienteProducto;
import com.empresa.crm.entities.Producto;
import com.empresa.crm.repositories.ClienteProductoRepository;
import com.empresa.crm.repositories.ClienteRepository;
import com.empresa.crm.repositories.ProductoRepository;
import com.empresa.crm.services.ClienteProductoService;
import com.empresa.crm.tenant.TenantContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClienteProductoServiceImpl implements ClienteProductoService {

	private final ClienteProductoRepository clienteProductoRepository;
	private final ClienteRepository clienteRepo;
	private final ProductoRepository productoRepo;

	@Override
	@Transactional(readOnly = true)
	public List<ProductoPendienteDto> listarPendientesPorCliente(Long clienteId) {
		String empresa = TenantContext.get();
		if (empresa == null || empresa.isBlank()) {
			throw new IllegalArgumentException("Empresa no definida");
		}
		return clienteProductoRepository.findPendientesPorCliente(clienteId, empresa);
	}

	@Override
	@Transactional
	public ClienteProducto asignarProductoACliente(ClienteProductoAsignarDTO dto) {
		String empresa = TenantContext.get();
		if (empresa == null || empresa.isBlank()) {
			throw new IllegalArgumentException("Empresa no definida");
		}

		if (dto.getClienteId() == null || dto.getProductoId() == null) {
			throw new IllegalArgumentException("clienteId y productoId son obligatorios");
		}

		int total = (dto.getCantidadTotal() == null ? 1 : dto.getCantidadTotal());
		if (total <= 0) {
			throw new IllegalArgumentException("cantidadTotal debe ser > 0");
		}

		Cliente cliente = clienteRepo.findById(dto.getClienteId())
				.orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));

		Producto producto = productoRepo.findById(dto.getProductoId())
				.orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));

		// ✅ Seguridad tenant (no mezclar empresas)
		if (!empresa.equalsIgnoreCase(cliente.getEmpresa())) {
			throw new IllegalArgumentException("El cliente no pertenece a la empresa " + empresa);
		}
		if (!empresa.equalsIgnoreCase(producto.getEmpresa())) {
			throw new IllegalArgumentException("El producto no pertenece a la empresa " + empresa);
		}

		return clienteProductoRepository
				.findByEmpresaAndClienteIdAndProductoId(empresa, cliente.getId(), producto.getId()).map(cp -> {
					// Si ya existe, puedes decidir si acumular o mantener:
					cp.setCantidadTotal(cp.getCantidadTotal() + total);
					return clienteProductoRepository.save(cp);
				}).orElseGet(() -> {
					ClienteProducto nuevo = new ClienteProducto();
					nuevo.setEmpresa(empresa);
					nuevo.setCliente(cliente);
					nuevo.setProducto(producto);
					nuevo.setFecha(LocalDateTime.now());
					nuevo.setEstado("PENDIENTE");
					nuevo.setCantidadTotal(total);
					nuevo.setCantidadEntregada(0);
					nuevo.setEntregado(false);
					return clienteProductoRepository.save(nuevo);
				});
	}

	@Override
	@Transactional(readOnly = true)
	public List<ClienteProducto> listarPorCliente(Long clienteId) {
		String empresa = TenantContext.get();
		if (empresa == null || empresa.isBlank()) {
			throw new IllegalArgumentException("Empresa no definida");
		}
		return clienteProductoRepository.findAllByEmpresaAndClienteId(empresa, clienteId);
	}

	@Override
	@Transactional
	public void eliminarAsignacion(Long clienteId, Long productoId) {
		String empresa = TenantContext.get();
		if (empresa == null || empresa.isBlank()) {
			throw new IllegalArgumentException("Empresa no definida");
		}
		clienteProductoRepository.deleteByEmpresaAndClienteIdAndProductoId(empresa, clienteId, productoId);
	}

	@Override
	@Transactional
	public ClienteProducto ensureAsignacion(Long clienteId, Long productoId, Integer cantidadSolicitada) {
		String empresa = TenantContext.get();
		if (empresa == null || empresa.isBlank()) {
			throw new IllegalArgumentException("Empresa no definida");
		}

		int total = (cantidadSolicitada == null || cantidadSolicitada <= 0) ? 1 : cantidadSolicitada;

		Cliente cliente = clienteRepo.findById(clienteId)
				.orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado: " + clienteId));

		Producto producto = productoRepo.findById(productoId)
				.orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + productoId));

		// ✅ Seguridad tenant
		if (!empresa.equalsIgnoreCase(cliente.getEmpresa())) {
			throw new IllegalArgumentException("Cliente " + clienteId + " no pertenece a " + empresa);
		}
		if (!empresa.equalsIgnoreCase(producto.getEmpresa())) {
			throw new IllegalArgumentException("Producto " + productoId + " no pertenece a " + empresa);
		}

		return clienteProductoRepository.findByEmpresaAndClienteIdAndProductoId(empresa, clienteId, productoId)
				.orElseGet(() -> {
					// ✅ Autocreación: el usuario final NO toca SQL
					ClienteProducto nuevo = new ClienteProducto();
					nuevo.setEmpresa(empresa);
					nuevo.setCliente(cliente);
					nuevo.setProducto(producto);
					nuevo.setFecha(LocalDateTime.now());
					nuevo.setEstado("PENDIENTE");
					nuevo.setCantidadTotal(total);
					nuevo.setCantidadEntregada(0);
					nuevo.setEntregado(false);
					return clienteProductoRepository.save(nuevo);
				});
	}
}
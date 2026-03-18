package com.empresa.crm.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.empresa.crm.dto.AjusteStockRequest;
import com.empresa.crm.entities.Producto;
import com.empresa.crm.entities.ProductoMovimiento;
import com.empresa.crm.repositories.ProductoMovimientoRepository;
import com.empresa.crm.repositories.ProductoRepository;
import com.empresa.crm.services.ProductoService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductoServiceImpl implements ProductoService {

	private final ProductoRepository repo;
	private final ProductoMovimientoRepository movimientoRepo;

	@Override
	@Transactional(readOnly = true)
	public List<Producto> listarPorEmpresa(String empresa) {
		if (empresa == null || empresa.isBlank()) {
			throw new IllegalArgumentException("Empresa no definida");
		}

		return repo.findByEmpresa(empresa);
	}

	@Override
	@Transactional
	public Producto crearProducto(Producto producto, String empresa) {
		if (empresa == null || empresa.isBlank()) {
			throw new IllegalArgumentException("Empresa no definida");
		}

		if (producto.getCodigo() == null || producto.getCodigo().isBlank()) {
			throw new IllegalArgumentException("El código es obligatorio");
		}

		if (producto.getNombre() == null || producto.getNombre().isBlank()) {
			throw new IllegalArgumentException("El nombre es obligatorio");
		}

		if (repo.findByCodigo(producto.getCodigo().trim()).isPresent()) {
			throw new IllegalArgumentException("Ya existe un producto con ese código");
		}

		if (producto.getStock() < 0) {
			throw new IllegalArgumentException("El stock no puede ser negativo");
		}

		if (producto.getPrecioSinIva() != null && producto.getPrecioSinIva() < 0) {
			throw new IllegalArgumentException("El precio sin IVA no puede ser negativo");
		}

		producto.setCodigo(producto.getCodigo().trim());
		producto.setNombre(producto.getNombre().trim());
		producto.setModelo(producto.getModelo() != null ? producto.getModelo().trim() : null);
		producto.setPrecioSinIva(producto.getPrecioSinIva() != null ? producto.getPrecioSinIva() : 0.0);
		producto.setEmpresa(empresa);

		return repo.save(producto);
	}

	@Override
	@Transactional
	public Producto actualizarProducto(Long id, Producto producto, String empresa) {
		if (empresa == null || empresa.isBlank()) {
			throw new IllegalArgumentException("Empresa no definida");
		}

		Producto existente = repo.findByIdAndEmpresa(id, empresa)
				.orElseThrow(() -> new IllegalArgumentException("Producto no encontrado para empresa " + empresa));

		if (producto.getCodigo() == null || producto.getCodigo().isBlank()) {
			throw new IllegalArgumentException("El código es obligatorio");
		}

		if (producto.getNombre() == null || producto.getNombre().isBlank()) {
			throw new IllegalArgumentException("El nombre es obligatorio");
		}

		if (producto.getStock() < 0) {
			throw new IllegalArgumentException("El stock no puede ser negativo");
		}

		if (producto.getPrecioSinIva() != null && producto.getPrecioSinIva() < 0) {
			throw new IllegalArgumentException("El precio sin IVA no puede ser negativo");
		}

		repo.findByCodigo(producto.getCodigo().trim()).ifPresent(prodConMismoCodigo -> {
			if (!prodConMismoCodigo.getId().equals(id)) {
				throw new IllegalArgumentException("Ya existe otro producto con ese código");
			}
		});

		existente.setCodigo(producto.getCodigo().trim());
		existente.setNombre(producto.getNombre().trim());
		existente.setModelo(producto.getModelo() != null ? producto.getModelo().trim() : null);
		existente.setStock(producto.getStock());
		existente.setPrecioSinIva(producto.getPrecioSinIva() != null ? producto.getPrecioSinIva() : 0.0);
		existente.setEmpresa(empresa);

		return repo.save(existente);
	}

	@Override
	@Transactional
	public Producto ajustarStock(Long id, AjusteStockRequest request, String empresa) {
		if (empresa == null || empresa.isBlank()) {
			throw new IllegalArgumentException("Empresa no definida");
		}

		Producto producto = repo.findByIdAndEmpresa(id, empresa)
				.orElseThrow(() -> new IllegalArgumentException("Producto no encontrado para empresa " + empresa));

		if (request == null || request.getDelta() == null) {
			throw new IllegalArgumentException("Falta 'delta'");
		}

		int delta = request.getDelta();

		if (delta == 0) {
			throw new IllegalArgumentException("'delta' no puede ser 0");
		}

		int stockAnterior = producto.getStock();
		int stockNuevo = stockAnterior + delta;

		if (stockNuevo < 0) {
			throw new IllegalArgumentException("El stock no puede quedar negativo");
		}

		producto.setStock(stockNuevo);
		Producto productoGuardado = repo.save(producto);

		ProductoMovimiento movimiento = new ProductoMovimiento();
		movimiento.setEmpresa(empresa);
		movimiento.setProducto(productoGuardado);
		movimiento.setTipo(delta > 0 ? "ENTRADA" : "SALIDA");
		movimiento.setCantidad(Math.abs(delta));
		movimiento.setStockAnterior(stockAnterior);
		movimiento.setStockNuevo(stockNuevo);
		movimiento.setMotivo(
				request.getMotivo() != null && !request.getMotivo().isBlank() ? request.getMotivo().trim() : null);
		movimiento.setFecha(LocalDateTime.now());

		movimientoRepo.save(movimiento);

		return productoGuardado;
	}

	@Override
	@Transactional(readOnly = true)
	public List<ProductoMovimiento> listarMovimientosPorProducto(Long productoId, String empresa) {
		if (empresa == null || empresa.isBlank()) {
			throw new IllegalArgumentException("Empresa no definida");
		}

		repo.findByIdAndEmpresa(productoId, empresa)
				.orElseThrow(() -> new IllegalArgumentException("Producto no encontrado para empresa " + empresa));

		return movimientoRepo.findByEmpresaAndProductoIdOrderByFechaDesc(empresa, productoId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<ProductoMovimiento> listarTodosLosMovimientos(String empresa) {
		if (empresa == null || empresa.isBlank()) {
			throw new IllegalArgumentException("Empresa no definida");
		}

		return movimientoRepo.findByEmpresaOrderByFechaDesc(empresa);
	}
}
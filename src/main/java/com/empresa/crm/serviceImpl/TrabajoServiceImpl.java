package com.empresa.crm.serviceImpl;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.empresa.crm.dto.TrabajoDTO;
import com.empresa.crm.entities.Producto;
import com.empresa.crm.entities.Trabajo;
import com.empresa.crm.repositories.ProductoRepository;
import com.empresa.crm.repositories.TrabajoRepository;
import com.empresa.crm.services.TrabajoService;

import jakarta.transaction.Transactional;

@Service
public class TrabajoServiceImpl implements TrabajoService {

	private final TrabajoRepository trabajoRepository;
	private final ProductoRepository productoRepo;

	public TrabajoServiceImpl(TrabajoRepository trabajoRepository, ProductoRepository productoRepo) {
		this.trabajoRepository = trabajoRepository;
		this.productoRepo = productoRepo;
	}

	@Override
	public List<Trabajo> findAll() {
		return trabajoRepository.findAll();
	}

	@Override
	public Trabajo findById(Long id) {
		return trabajoRepository.findById(id).orElse(null);
	}

	/**
	 * ✅ FIX PRECIO: Si el trabajo viene de un producto (productoId != null), asigna
	 * precioUnitario = producto.precioSinIva antes de guardar.
	 */
	@Override
	@Transactional
	public Trabajo save(Trabajo trabajo) {

		if (trabajo.getProductoId() != null) {

			Producto producto = productoRepo.findById(trabajo.getProductoId())
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));

			// Si el trabajo no tiene empresa, la heredamos del producto
			if (trabajo.getEmpresa() == null || trabajo.getEmpresa().isBlank()) {
				trabajo.setEmpresa(producto.getEmpresa());
			}

			// Validación: empresa del trabajo debe coincidir con la del producto
			if (producto.getEmpresa() != null && trabajo.getEmpresa() != null
					&& !producto.getEmpresa().equalsIgnoreCase(trabajo.getEmpresa())) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empresa no coincide (trabajo="
						+ trabajo.getEmpresa() + ", producto=" + producto.getEmpresa() + ")");
			}

			// ✅ Asignar precio SIEMPRE desde el producto
			trabajo.setPrecioUnitario(producto.getPrecioSinIva() != null ? producto.getPrecioSinIva() : 0.0);

			// (Opcional) si quieres asegurar descripción = nombre del producto
			if (trabajo.getDescripcion() == null || trabajo.getDescripcion().isBlank()) {
				trabajo.setDescripcion(producto.getNombre());
			}
		}

		return trabajoRepository.save(trabajo);
	}

	@Override
	@Transactional
	public void deleteById(Long id) {

		Trabajo t = trabajoRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trabajo no existe"));

		if (t.isEntregado()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"No se puede eliminar: el trabajo ya está entregado");
		}

		Long productoId = t.getProductoId();
		if (productoId != null) {
			int cantidad = (t.getUnidades() != null && t.getUnidades() > 0) ? t.getUnidades() : 1;

			int updated = productoRepo.incrementStockByEmpresa(productoId, cantidad, t.getEmpresa());
			if (updated == 0) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"No se pudo reponer stock (producto/empresa no coinciden)");
			}
		}

		trabajoRepository.delete(t);
	}

	@Override
	public List<Trabajo> findByProveedor(Long proveedorId) {
		return trabajoRepository.findByProveedorId(proveedorId);
	}

	@Override
	public List<Trabajo> findByPagado(boolean pagado) {
		return trabajoRepository.findByPagado(pagado);
	}

	@Override
	public List<Trabajo> findByCliente(Long clienteId) {
		return trabajoRepository.findByCliente_Id(clienteId);
	}

	// ✅ NUEVO: DTO COMPLETO PARA LISTADO DE CLIENTE (para que Angular pinte
	// precio/importe)
	@Override
	public List<TrabajoDTO> findDtoByCliente(Long clienteId) {

		List<Trabajo> trabajos = trabajoRepository.findByCliente_Id(clienteId);

		return trabajos.stream().map(t -> {

			TrabajoDTO dto = new TrabajoDTO();
			dto.setId(t.getId());
			dto.setProductoId(t.getProductoId());

			// ✅ Si hay producto asociado, sacamos codigo/nombre del producto
			if (t.getProductoId() != null) {
				Producto p = productoRepo.findById(t.getProductoId()).orElse(null);
				if (p != null) {
					dto.setCodigo(p.getCodigo());
					dto.setNombre(p.getNombre());
				} else {
					// fallback si el producto fue borrado o no existe
					dto.setCodigo(null);
					dto.setNombre(t.getDescripcion());
				}
			} else {
				// ✅ trabajo manual (sin producto)
				dto.setCodigo(null);
				dto.setNombre(t.getDescripcion());
			}

			dto.setCantidad(t.getUnidades());
			dto.setPrecioUnitario(t.getPrecioUnitario());
			dto.setDescuento(t.getDescuento());
			dto.setImporte(t.getImporte());

			dto.setPagado(t.getImportePagado());

			dto.setEntregado(t.isEntregado());
			if (t.getFechaEntrega() != null) {
				dto.setFechaEntrega(t.getFechaEntrega().toLocalDate());
			}
			dto.setEmpresa(t.getEmpresa());

			return dto;

		}).collect(java.util.stream.Collectors.toList());
	}

	@Override
	@Transactional
	public void deleteProductoCliente(Long clienteId, Long productoId, String empresa) {
		if (clienteId == null || productoId == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "clienteId y productoId son obligatorios");
		}
		if (empresa == null || empresa.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "empresa es obligatoria");
		}

		Trabajo t = trabajoRepository.findByClienteIdAndProductoIdAndEntregadoFalse(clienteId, productoId).stream()
				.filter(x -> empresa.equalsIgnoreCase(x.getEmpresa())).findFirst()
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"No hay trabajo pendiente para borrar"));

		int cantidad = (t.getUnidades() != null && t.getUnidades() > 0) ? t.getUnidades() : 1;

		int updated = productoRepo.incrementStockByEmpresa(productoId, cantidad, empresa);
		if (updated == 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"No se pudo reponer stock (empresa/producto incorrectos)");
		}

		trabajoRepository.delete(t);
	}
}
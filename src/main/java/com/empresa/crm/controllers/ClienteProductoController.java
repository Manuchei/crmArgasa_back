package com.empresa.crm.controllers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.empresa.crm.dto.AddProductoRequest;
import com.empresa.crm.dto.ClienteProductoAsignarDTO;
import com.empresa.crm.dto.ClienteProductoCompradoDTO;
import com.empresa.crm.entities.Cliente;
import com.empresa.crm.entities.ClienteProducto;
import com.empresa.crm.entities.Producto;
import com.empresa.crm.entities.Trabajo;
import com.empresa.crm.repositories.ClienteRepository;
import com.empresa.crm.repositories.ProductoRepository;
import com.empresa.crm.repositories.TrabajoRepository;
import com.empresa.crm.serviceImpl.ClienteProductoServiceImpl;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;

@RestController
@RequestMapping("/api/clientes")
@CrossOrigin(origins = "http://localhost:4200")
public class ClienteProductoController {

	private final com.empresa.crm.services.TrabajoService trabajoService;
	private final ClienteRepository clienteRepo;
	private final ProductoRepository productoRepo;
	private final TrabajoRepository trabajoRepo;
	private final ClienteProductoServiceImpl clienteProductoService;

	public ClienteProductoController(com.empresa.crm.services.TrabajoService trabajoService,
			ClienteRepository clienteRepo, ProductoRepository productoRepo, TrabajoRepository trabajoRepo,
			ClienteProductoServiceImpl clienteProductoService) {
		this.trabajoService = trabajoService;
		this.clienteRepo = clienteRepo;
		this.productoRepo = productoRepo;
		this.trabajoRepo = trabajoRepo;
		this.clienteProductoService = clienteProductoService;
	}

	@GetMapping("/{clienteId}/productos/pendientes")
	public ResponseEntity<?> getProductosPendientes(@PathVariable Long clienteId, HttpServletRequest request) {

		Cliente c = clienteRepo.findById(clienteId).orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

		String empresaHeader = request.getHeader("X-Empresa");
		String empresa = (empresaHeader != null && !empresaHeader.isBlank()) ? empresaHeader.trim() : c.getEmpresa();

		if (empresa == null || empresa.isBlank()) {
			return ResponseEntity.badRequest().body("Empresa no determinada");
		}

		// ✅ Importante: si usas TenantContext en servicios, ponlo aquí también
		// (si ya lo pones en un filtro/interceptor, entonces NO hace falta)
		// TenantContext.set(empresa);

		return ResponseEntity.ok(clienteProductoService.listarPendientesPorCliente(clienteId));
	}

	@PostMapping("/{clienteId}/productos/{productoId}")
	@Transactional
	public Trabajo addProductoCliente(@PathVariable Long clienteId, @PathVariable Long productoId,
			@RequestBody(required = false) AddProductoRequest req,
			@RequestHeader(value = "X-Empresa", required = false) String empresaHeader) {

		if (req == null)
			req = new AddProductoRequest();

		// empresa: header > cliente
		Cliente cliente = clienteRepo.findById(clienteId)
				.orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

		String empresa = (empresaHeader != null && !empresaHeader.isBlank()) ? empresaHeader.trim()
				: cliente.getEmpresa();

		if (empresa == null || empresa.isBlank()) {
			throw new RuntimeException("Empresa no determinada");
		}

		int cantidad = req.getCantidad() != null ? req.getCantidad() : 1;
		if (cantidad <= 0)
			cantidad = 1;

		Producto p = productoRepo.findById(productoId).orElseThrow(() -> new RuntimeException("Producto no existe"));

		if (!p.getEmpresa().equalsIgnoreCase(empresa)) {
			throw new RuntimeException("Producto no pertenece a la empresa activa");
		}

		int updated = productoRepo.decrementStockIfAvailable(productoId, cantidad, empresa);
		if (updated == 0) {
			throw new RuntimeException("Stock insuficiente");
		}

		Trabajo t = new Trabajo();
		t.setCliente(cliente);
		t.setProductoId(productoId);
		t.setUnidades(cantidad);
		t.setDescuento(req.getDescuento() != null ? req.getDescuento() : 0.0);
		t.setImportePagado(req.getImportePagado() != null ? req.getImportePagado() : 0.0);
		t.setEmpresa(empresa);

		// descripción automática
		t.setDescripcion(p.getNombre());

		// ✅ Mantener cliente_producto sincronizado con las compras
		ClienteProductoAsignarDTO dto = new ClienteProductoAsignarDTO();
		dto.setClienteId(clienteId);
		dto.setProductoId(productoId);
		dto.setCantidadTotal(cantidad);

		clienteProductoService.asignarProductoACliente(dto);

		return trabajoService.save(t);
	}

	@GetMapping("/{clienteId}/productos")
	public ResponseEntity<?> getProductosComprados(@PathVariable Long clienteId, HttpServletRequest request) {
		Cliente c = clienteRepo.findById(clienteId).orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

		String empresaHeader = request.getHeader("X-Empresa");
		String empresa = (empresaHeader != null && !empresaHeader.isBlank()) ? empresaHeader.trim() : c.getEmpresa();

		if (empresa == null || empresa.isBlank()) {
			return ResponseEntity.badRequest().body("Empresa no determinada");
		}

		List<Trabajo> trabajos = trabajoRepo.findByClienteIdAndEmpresa(clienteId, empresa).stream()
				.filter(t -> t.getProductoId() != null).collect(Collectors.toList());

		if (trabajos.isEmpty()) {
			return ResponseEntity.ok(List.of());
		}

		Map<Long, Integer> unidadesPorProducto = new HashMap<>();
		for (Trabajo t : trabajos) {
			Long pid = t.getProductoId();
			int u = (t.getUnidades() != null) ? t.getUnidades() : 0;
			unidadesPorProducto.merge(pid, u, Integer::sum);
		}

		List<Long> ids = new ArrayList<>(unidadesPorProducto.keySet());
		List<Producto> productos = productoRepo.findAllById(ids);

		List<ClienteProductoCompradoDTO> res = productos.stream()
				.map(p -> new ClienteProductoCompradoDTO(p.getId(), p.getCodigo(), p.getNombre(),
						unidadesPorProducto.getOrDefault(p.getId(), 0), false, null))
				.sorted(Comparator.comparing(ClienteProductoCompradoDTO::getNombre, String.CASE_INSENSITIVE_ORDER))
				.collect(Collectors.toList());

		return ResponseEntity.ok(res);
	}

	/**
	 * ✅ ELIMINAR producto del cliente (borra un trabajo pendiente asociado a ese
	 * producto) y repone stock.
	 */
	@DeleteMapping("/{clienteId}/productos/{productoId}")
	public ResponseEntity<Void> deleteProductoCliente(@PathVariable Long clienteId,
	        @PathVariable Long productoId,
	        @RequestHeader(value = "X-Empresa", required = false) String empresaHeader) {

	    Cliente c = clienteRepo.findById(clienteId)
	            .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

	    String empresa = (empresaHeader != null && !empresaHeader.isBlank())
	            ? empresaHeader.trim()
	            : c.getEmpresa();

	    if (empresa == null || empresa.isBlank()) {
	        throw new RuntimeException("Empresa no determinada");
	    }

	    // 1. Borra el trabajo/pedido del cliente
	    trabajoService.deleteProductoCliente(clienteId, productoId, empresa);

	    // 2. Borra también la asignación pendiente para que no aparezca en rutas
	    clienteProductoService.eliminarAsignacion(clienteId, productoId);

	    return ResponseEntity.noContent().build();
	}
	@PostMapping("/asignar")
	public ResponseEntity<ClienteProducto> asignar(@RequestBody ClienteProductoAsignarDTO dto) {
		return ResponseEntity.ok(clienteProductoService.asignarProductoACliente(dto));
	}

	@GetMapping("/cliente/{clienteId}")
	public ResponseEntity<?> listado(@PathVariable Long clienteId) {
		return ResponseEntity.ok(clienteProductoService.listarPorCliente(clienteId));
	}

	@DeleteMapping("/cliente/{clienteId}/producto/{productoId}")
	public ResponseEntity<Void> eliminar(@PathVariable Long clienteId, @PathVariable Long productoId) {
		clienteProductoService.eliminarAsignacion(clienteId, productoId);
		return ResponseEntity.noContent().build();

	}
}
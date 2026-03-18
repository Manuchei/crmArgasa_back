package com.empresa.crm.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.empresa.crm.dto.AjusteStockRequest;
import com.empresa.crm.entities.Producto;
import com.empresa.crm.services.ProductoService;
import com.empresa.crm.tenant.TenantContext;

@RestController
@RequestMapping("/api/productos")
@CrossOrigin(origins = "http://localhost:4200", allowedHeaders = { "Authorization", "Content-Type",
		"X-Empresa" }, methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH,
				RequestMethod.DELETE, RequestMethod.OPTIONS })
public class ProductoController {

	private final ProductoService productoService;

	public ProductoController(ProductoService productoService) {
		this.productoService = productoService;
	}

	@GetMapping
	public ResponseEntity<?> list() {
		try {
			String empresa = TenantContext.get();
			return ResponseEntity.ok(productoService.listarPorEmpresa(empresa));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	@PostMapping
	public ResponseEntity<?> create(@RequestBody Producto p) {
		try {
			String empresa = TenantContext.get();
			return ResponseEntity.ok(productoService.crearProducto(p, empresa));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	@PutMapping("/{id}")
	public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Producto p) {
		try {
			String empresa = TenantContext.get();
			return ResponseEntity.ok(productoService.actualizarProducto(id, p, empresa));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	@PatchMapping("/{id}/stock")
	public ResponseEntity<?> ajustarStock(@PathVariable Long id, @RequestBody AjusteStockRequest request) {
		try {
			String empresa = TenantContext.get();
			return ResponseEntity.ok(productoService.ajustarStock(id, request, empresa));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	@GetMapping("/{id}/movimientos")
	public ResponseEntity<?> listarMovimientosPorProducto(@PathVariable Long id) {
		try {
			String empresa = TenantContext.get();
			return ResponseEntity.ok(productoService.listarMovimientosPorProducto(id, empresa));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	@GetMapping("/movimientos")
	public ResponseEntity<?> listarTodosLosMovimientos() {
		try {
			String empresa = TenantContext.get();
			return ResponseEntity.ok(productoService.listarTodosLosMovimientos(empresa));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}
}
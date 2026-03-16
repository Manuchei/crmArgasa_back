package com.empresa.crm.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.empresa.crm.entities.Producto;
import com.empresa.crm.repositories.ProductoRepository;
import com.empresa.crm.tenant.TenantContext;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/productos")
@CrossOrigin(origins = "http://localhost:4200", allowedHeaders = { "Authorization", "Content-Type",
		"X-Empresa" }, methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH,
				RequestMethod.DELETE, RequestMethod.OPTIONS })
public class ProductoController {

	private final ProductoRepository repo;

	public ProductoController(ProductoRepository repo) {
		this.repo = repo;
	}

	@GetMapping
	public ResponseEntity<?> list() {
		String empresa = TenantContext.get();

		if (empresa == null || empresa.isBlank()) {
			return ResponseEntity.badRequest().body("Empresa no definida");
		}

		List<Producto> productos = repo.findByEmpresa(empresa);
		return ResponseEntity.ok(productos);
	}

	@PostMapping
	public ResponseEntity<?> create(@RequestBody Producto p, HttpServletRequest request) {
		String empresa = TenantContext.get();

		if (empresa == null || empresa.isBlank()) {
			return ResponseEntity.badRequest().body("Empresa no definida");
		}

		if (p.getCodigo() == null || p.getCodigo().isBlank()) {
			return ResponseEntity.badRequest().body("El código es obligatorio");
		}

		if (p.getNombre() == null || p.getNombre().isBlank()) {
			return ResponseEntity.badRequest().body("El nombre es obligatorio");
		}

		// Si precio y stock son primitivos, ya vienen con valor por defecto:
		// double -> 0.0
		// int -> 0
		// Así que no hace falta comprobar null

		p.setEmpresa(empresa);

		return ResponseEntity.ok(repo.save(p));
	}

	@PatchMapping("/{id}/stock")
	public ResponseEntity<?> ajustarStock(@PathVariable Long id, @RequestBody Map<String, Object> body) {
		String empresa = TenantContext.get();

		if (empresa == null || empresa.isBlank()) {
			return ResponseEntity.badRequest().body("Empresa no definida");
		}

		Producto prod = repo.findByIdAndEmpresa(id, empresa)
				.orElseThrow(() -> new RuntimeException("Producto no encontrado para empresa " + empresa));

		Object deltaObj = body.get("delta");
		if (deltaObj == null) {
			return ResponseEntity.badRequest().body("Falta 'delta'");
		}

		int delta;
		try {
			delta = (deltaObj instanceof Number) ? ((Number) deltaObj).intValue()
					: Integer.parseInt(String.valueOf(deltaObj));
		} catch (Exception e) {
			return ResponseEntity.badRequest().body("'delta' debe ser numérico");
		}

		int stockActual = prod.getStock();
		int nuevoStock = stockActual + delta;

		if (nuevoStock < 0) {
			return ResponseEntity.badRequest().body("El stock no puede quedar negativo");
		}

		prod.setStock(nuevoStock);

		return ResponseEntity.ok(repo.save(prod));
	}
}
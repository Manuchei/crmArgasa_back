package com.empresa.crm.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.empresa.crm.entities.Producto;
import com.empresa.crm.repositories.ProductoRepository;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/productos")
@CrossOrigin(origins = "http://localhost:4200")
public class ProductoController {

	private final ProductoRepository repo;

	public ProductoController(ProductoRepository repo) {
		this.repo = repo;
	}

	@GetMapping
	public List<Producto> list(@RequestParam(required = false) String empresa) {
		if (empresa != null && !empresa.isBlank())
			return repo.findByEmpresa(empresa);
		return repo.findAll();
	}

	@PostMapping
	public ResponseEntity<?> create(@RequestBody Producto p, HttpServletRequest request) {

		System.out.println("CONTENT-TYPE RECIBIDO: " + request.getContentType());
		System.out.println("LLEGA -> " + p);

		if (p.getCodigo() == null || p.getCodigo().isBlank()) {
			return ResponseEntity.badRequest().body("El código es obligatorio");
		}

		return ResponseEntity.ok(repo.save(p));
	}

	// ✅ NUEVO: ajustar stock por delta (+/-)
	@PatchMapping("/{id}/stock")
	public ResponseEntity<?> ajustarStock(@PathVariable Long id, @RequestBody Map<String, Object> body) {
		Producto prod = repo.findById(id).orElseThrow(() -> new RuntimeException("Producto no encontrado"));

		Object deltaObj = body.get("delta");
		if (deltaObj == null)
			return ResponseEntity.badRequest().body("Falta 'delta'");

		int delta;
		try {
			delta = (deltaObj instanceof Number) ? ((Number) deltaObj).intValue()
					: Integer.parseInt(String.valueOf(deltaObj));
		} catch (Exception e) {
			return ResponseEntity.badRequest().body("'delta' debe ser numérico");
		}

		int actual = prod.getStock();
		int nuevo = actual + delta;

		if (nuevo < 0) {
			return ResponseEntity.badRequest().body("El stock no puede quedar negativo");
		}

		prod.setStock(nuevo);
		Producto guardado = repo.save(prod);

		return ResponseEntity.ok(guardado);
	}
}

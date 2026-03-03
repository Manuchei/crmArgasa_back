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
@CrossOrigin(origins = "http://localhost:4200")
public class ProductoController {

	private final ProductoRepository repo;

	public ProductoController(ProductoRepository repo) {
		this.repo = repo;
	}

	@GetMapping
	public List<Producto> list() {
		String empresa = TenantContext.get();
		if (empresa == null || empresa.isBlank()) {
			throw new IllegalArgumentException("Empresa no definida");
		}
		return repo.findByEmpresa(empresa);
	}

	@PostMapping
	public ResponseEntity<?> create(@RequestBody Producto p, HttpServletRequest request) {

		String empresa = TenantContext.get();
		if (empresa == null || empresa.isBlank()) {
			return ResponseEntity.badRequest().body("Empresa no definida");
		}

		// ✅ forzamos empresa del tenant (evita que te cuelen otra)
		p.setEmpresa(empresa);

		if (p.getCodigo() == null || p.getCodigo().isBlank()) {
			return ResponseEntity.badRequest().body("El código es obligatorio");
		}

		return ResponseEntity.ok(repo.save(p));
	}

	@PatchMapping("/{id}/stock")
	public ResponseEntity<?> ajustarStock(@PathVariable Long id, @RequestBody Map<String, Object> body) {

		String empresa = TenantContext.get();
		if (empresa == null || empresa.isBlank()) {
			return ResponseEntity.badRequest().body("Empresa no definida");
		}

		// ✅ seguridad: solo si es de esa empresa
		Producto prod = repo.findByIdAndEmpresa(id, empresa)
				.orElseThrow(() -> new RuntimeException("Producto no encontrado para empresa " + empresa));

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

		int nuevo = prod.getStock() + delta;
		if (nuevo < 0)
			return ResponseEntity.badRequest().body("El stock no puede quedar negativo");

		prod.setStock(nuevo);
		return ResponseEntity.ok(repo.save(prod));
	}
}
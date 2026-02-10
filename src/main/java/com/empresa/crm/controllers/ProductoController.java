package com.empresa.crm.controllers;

import java.util.List;

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
}

package com.empresa.crm.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.empresa.crm.dto.ProductoCreateDTO;
import com.empresa.crm.entities.Producto;
import com.empresa.crm.repositories.ProductoRepository;

import io.swagger.v3.oas.annotations.parameters.RequestBody;

@RestController
@RequestMapping("/api/productos")
@CrossOrigin(origins = "http://localhost:4200")
public class ProductoController {
	
	private final ProductoRepository repo;
	
	public ProductoController(ProductoRepository repo) {this.repo = repo;}
	
	@GetMapping
	public List<Producto> list(@RequestParam(required = false) String empresa){
		if (empresa != null && !empresa.isBlank()) return repo.findByEmpresa(empresa);
		return repo.findAll();
	}
	
	@PostMapping(consumes = "application/json", produces = "application/json")
	  public ResponseEntity<?> create(@RequestBody Producto p) {

	    System.out.println("LLEGA -> " + p);

	    if (p.getCodigo() == null || p.getCodigo().isBlank()) {
	      return ResponseEntity.badRequest().body("El código es obligatorio");
	    }

	    return ResponseEntity.ok(repo.save(p));
	  }
	}

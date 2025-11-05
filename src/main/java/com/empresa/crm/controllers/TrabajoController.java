package com.empresa.crm.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.empresa.crm.entities.Trabajo;
import com.empresa.crm.services.TrabajoService;

@RestController
@RequestMapping("/api/trabajos")
@CrossOrigin(origins = "http://localhost:4200")
public class TrabajoController {

	private final TrabajoService trabajoService;

	public TrabajoController(TrabajoService trabajoService) {
		this.trabajoService = trabajoService;
	}

	@GetMapping
	public List<Trabajo> listarTodos() {
		return trabajoService.findAll();
	}

	@GetMapping("/{id}")
	public Trabajo obtenerPorId(@PathVariable Long id) {
		return trabajoService.findById(id);
	}

	@PostMapping
	public Trabajo crear(@RequestBody Trabajo trabajo) {
		return trabajoService.save(trabajo);
	}

	@PutMapping("/{id}")
	public Trabajo actualizar(@PathVariable Long id, @RequestBody Trabajo trabajo) {
		trabajo.setId(id);
		return trabajoService.save(trabajo);
	}

	@DeleteMapping("/{id}")
	public void eliminar(@PathVariable Long id) {
		trabajoService.deleteById(id);
	}

	@GetMapping("/proveedor/{proveedorId}")
	public List<Trabajo> listarPorProveedor(@PathVariable Long proveedorId) {
		return trabajoService.findByProveedor(proveedorId);
	}

	@GetMapping("/pagado/{pagado}")
	public List<Trabajo> listarPorPago(@PathVariable boolean pagado) {
		return trabajoService.findByPagado(pagado);
	}
}
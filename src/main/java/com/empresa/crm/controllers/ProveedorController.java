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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.empresa.crm.entities.Proveedor;
import com.empresa.crm.repositories.ProveedorRepository;
import com.empresa.crm.services.ProveedorService;

@RestController
@RequestMapping("/api/proveedores")
@CrossOrigin(origins = "http://localhost:4200")
public class ProveedorController {

	private final ProveedorService proveedorService;
	private final ProveedorRepository proveedorRepository;

	public ProveedorController(ProveedorService proveedorService) {
		this.proveedorService = proveedorService;
		this.proveedorRepository = null;
	}

	@GetMapping
	public List<Proveedor> listarTodos() {
		return proveedorService.findAll();
	}

	@GetMapping("/{id}")
	public Proveedor obtenerPorId(@PathVariable Long id) {
		return proveedorService.findById(id);
	}

	@PostMapping
	public Proveedor crear(@RequestBody Proveedor proveedor) {
		return proveedorService.save(proveedor);
	}

	@PutMapping("/{id}")
	public Proveedor actualizar(@PathVariable Long id, @RequestBody Proveedor proveedor) {
		proveedor.setId(id);
		return proveedorService.save(proveedor);
	}

	@DeleteMapping("/{id}")
	public void eliminar(@PathVariable Long id) {
		proveedorService.deleteById(id);
	}

	@GetMapping("/oficio/{oficio}")
	public List<Proveedor> listarPorOficio(@PathVariable String oficio) {
		return proveedorService.findByOficio(oficio);
	}

	@GetMapping("/empresa/{empresa}")
	public List<Proveedor> listarPorEmpresa(@PathVariable String empresa) {
		return proveedorService.findByEmpresa(empresa);
	}

	@GetMapping("/buscar")
	public List<Proveedor> buscarProveedores(@RequestParam String texto,
			@RequestParam(required = false) String empresa) {

		if (empresa != null && !empresa.isBlank()) {
			return proveedorRepository.buscarPorNombreYEmpresa(texto, empresa);
		} else {
			return proveedorRepository.buscarPorNombreOApellido(texto);
		}
	}

	@GetMapping("/argasa")
	public List<Proveedor> getArgasa() {
		return proveedorRepository.findByTrabajaEnArgasaTrue();
	}

	@GetMapping("/luga")
	public List<Proveedor> getLuga() {
		return proveedorRepository.findByTrabajaEnLugaTrue();
	}

	@GetMapping("/buscar/{empresa}")
	public List<Proveedor> buscarPorEmpresa(@PathVariable String empresa, @RequestParam String texto) {
		return proveedorRepository.buscarPorNombreYEmpresa(texto, empresa);
	}

}
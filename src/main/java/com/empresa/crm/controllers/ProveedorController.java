package com.empresa.crm.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;

import com.empresa.crm.dto.ProveedorDTO;
import com.empresa.crm.entities.Proveedor;
import com.empresa.crm.services.ProveedorService;

@RestController
@RequestMapping("/api/proveedores")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class ProveedorController {

	private final ProveedorService proveedorService;

	@GetMapping
	public List<Proveedor> listarTodos() {
		return proveedorService.findAll();
	}

	@GetMapping("/{id}")
	public Proveedor obtenerPorId(@PathVariable Long id) {
		return proveedorService.findById(id);
	}

	@PostMapping
	public Proveedor crear(@RequestBody ProveedorDTO proveedorDto) {
		return proveedorService.saveFromDto(proveedorDto);
	}

	@PutMapping("/{id}")
	public Proveedor actualizar(@PathVariable Long id, @RequestBody ProveedorDTO proveedorDto) {
		proveedorDto.setId(id);
		return proveedorService.saveFromDto(proveedorDto);
	}

	@DeleteMapping("/{id}")
	public void eliminar(@PathVariable Long id) {
		proveedorService.deleteById(id);
	}

	@GetMapping("/buscar")
	public List<Proveedor> buscar(@RequestParam(required = false) String texto,
			@RequestParam(required = false) String empresa, @RequestParam(required = false) String oficio) {

		return proveedorService.buscar(texto == null ? "" : texto, empresa == null ? "" : empresa,
				oficio == null ? "" : oficio);
	}
}
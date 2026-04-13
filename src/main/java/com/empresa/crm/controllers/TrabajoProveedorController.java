package com.empresa.crm.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.empresa.crm.entities.Proveedor;
import com.empresa.crm.entities.Trabajo;
import com.empresa.crm.services.ProveedorService;
import com.empresa.crm.services.TrabajoService;

@RestController
@RequestMapping("/api/trabajos-proveedor")
@CrossOrigin(origins = "http://localhost:4200")
public class TrabajoProveedorController {

	private final TrabajoService trabajoService;
	private final ProveedorService proveedorService;

	public TrabajoProveedorController(TrabajoService trabajoService, ProveedorService proveedorService) {
		this.trabajoService = trabajoService;
		this.proveedorService = proveedorService;
	}

	@GetMapping("/proveedor/{proveedorId}")
	public List<Trabajo> getTrabajosPorProveedor(@PathVariable Long proveedorId) {
		return trabajoService.findByProveedor(proveedorId);
	}

	@PostMapping("/proveedor/{proveedorId}")
	public Trabajo crearTrabajoProveedor(@PathVariable Long proveedorId, @RequestBody Trabajo trabajo) {

		Proveedor proveedor = proveedorService.findById(proveedorId);

		if (proveedor == null) {
			throw new RuntimeException("Proveedor no encontrado con ID: " + proveedorId);
		}

		trabajo.setProveedor(proveedor);
		trabajo.setEmpresa(proveedor.getEmpresa());

		if (trabajo.getProductoId() == null) {
			Double importeManual = trabajo.getImporte() != null ? trabajo.getImporte() : 0.0;
			trabajo.setUnidades(1);
			trabajo.setPrecioUnitario(importeManual);
			trabajo.setDescuento(0.0);
		}

		if (trabajo.getImportePagado() == null) {
			trabajo.setImportePagado(0.0);
		}

		Trabajo guardado = trabajoService.save(trabajo);

		// ✅ fuerza recálculo real desde BD
		proveedorService.findById(proveedorId);

		return guardado;
	}

	@DeleteMapping("/{trabajoId}")
	public void eliminarTrabajoProveedor(@PathVariable Long trabajoId) {

		Trabajo trabajo = trabajoService.findById(trabajoId);

		if (trabajo == null) {
			throw new RuntimeException("Trabajo no encontrado con ID: " + trabajoId);
		}

		Long proveedorId = trabajo.getProveedor() != null ? trabajo.getProveedor().getId() : null;

		trabajoService.deleteById(trabajoId);

		if (proveedorId != null) {
			proveedorService.findById(proveedorId);
		}
	}
}
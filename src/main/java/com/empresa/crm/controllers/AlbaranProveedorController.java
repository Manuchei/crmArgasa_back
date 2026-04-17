package com.empresa.crm.controllers;

import java.time.LocalDate;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.empresa.crm.entities.AlbaranProveedor;
import com.empresa.crm.entities.LineaAlbaranProveedor;
import com.empresa.crm.services.AlbaranProveedorService;

@RestController
@RequestMapping("/api/albaranes-proveedor")
@CrossOrigin(origins = "http://localhost:4200")
public class AlbaranProveedorController {

	private final AlbaranProveedorService service;

	public AlbaranProveedorController(AlbaranProveedorService service) {
		this.service = service;
	}

	@GetMapping
	public List<AlbaranProveedor> listar(@RequestParam(required = false) Long proveedorId) {

		if (proveedorId != null)
			return service.findByProveedor(proveedorId);

		return service.findAll();
	}

	@GetMapping("/proveedores/{proveedorId}")
	public List<AlbaranProveedor> listarPorProveedor(@PathVariable Long proveedorId) {
		return service.findByProveedor(proveedorId);
	}

	@GetMapping("/{id}")
	public AlbaranProveedor detalle(@PathVariable Long id) {
		return service.findById(id);
	}

	@PostMapping("/proveedores/{proveedorId}")
	public AlbaranProveedor crearDesdeProveedor(@PathVariable Long proveedorId,
			@RequestBody(required = false) GenerarAlbaranProveedorRequest request) {

		String numeroProveedor = request != null ? request.getNumeroProveedor() : null;
		LocalDate fechaEmision = request != null ? request.getFechaEmision() : null;

		return service.crearDesdeProveedor(proveedorId, numeroProveedor, fechaEmision);
	}

	@PutMapping("/{id}")
	public AlbaranProveedor actualizar(@PathVariable Long id, @RequestBody AlbaranProveedor albaran) {
		albaran.setId(id);
		return service.save(albaran);
	}

	@DeleteMapping("/{id}")
	public void borrar(@PathVariable Long id) {
		service.deleteById(id);
	}

	@PostMapping("/{id}/lineas")
	public AlbaranProveedor addLinea(@PathVariable Long id, @RequestBody LineaAlbaranProveedor linea) {
		return service.agregarLinea(id, linea);
	}

	@DeleteMapping("/{id}/lineas/{lineaId}")
	public AlbaranProveedor deleteLinea(@PathVariable Long id, @PathVariable Long lineaId) {
		return service.eliminarLinea(id, lineaId);
	}

	@PostMapping("/{id}/confirmar")
	public AlbaranProveedor confirmar(@PathVariable Long id) {
		return service.confirmar(id);
	}

	public static class GenerarAlbaranProveedorRequest {
		private String numeroProveedor;
		private LocalDate fechaEmision;

		public String getNumeroProveedor() {
			return numeroProveedor;
		}

		public void setNumeroProveedor(String numeroProveedor) {
			this.numeroProveedor = numeroProveedor;
		}

		public LocalDate getFechaEmision() {
			return fechaEmision;
		}

		public void setFechaEmision(LocalDate fechaEmision) {
			this.fechaEmision = fechaEmision;
		}
	}
}
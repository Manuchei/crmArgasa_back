package com.empresa.crm.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.empresa.crm.entities.FacturaProveedor;
import com.empresa.crm.services.FacturaProveedorService;

@RestController
@RequestMapping("/api/facturas")
@CrossOrigin(origins = "http://localhost:4200")
public class FacturaProveedorController {

	private final FacturaProveedorService facturaService;

	public FacturaProveedorController(FacturaProveedorService facturaService) {
		this.facturaService = facturaService;
	}

	@GetMapping
	public List<FacturaProveedor> listarTodas() {
		return facturaService.findAll();
	}

	@GetMapping("/proveedor/{proveedorId}")
	public List<FacturaProveedor> listarPorProveedor(@PathVariable Long proveedorId) {
		return facturaService.findByProveedor(proveedorId);
	}

	@PostMapping("/generar/{proveedorId}/{empresa}")
	public FacturaProveedor generar(@PathVariable Long proveedorId, @PathVariable String empresa) {
		return facturaService.generarFactura(proveedorId, empresa);
	}

	@PutMapping("/pagar/{facturaId}")
	public FacturaProveedor pagar(@PathVariable Long facturaId) {
		return facturaService.marcarComoPagada(facturaId);
	}

	@GetMapping("/empresa/{empresa}")
	public List<FacturaProveedor> listarPorEmpresa(@PathVariable String empresa) {
		return facturaService.findByEmpresa(empresa);
	}
}

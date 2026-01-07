package com.empresa.crm.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.empresa.crm.entities.FacturaCliente;
import com.empresa.crm.services.FacturaClienteService;

@RestController
@RequestMapping("/api/facturas-clientes")
@CrossOrigin(origins = "http://localhost:4200")
public class FacturaClienteController {

	private final FacturaClienteService facturaService;

	public FacturaClienteController(FacturaClienteService facturaService) {
		this.facturaService = facturaService;
	}

	@GetMapping
	public List<FacturaCliente> listarTodas() {
		return facturaService.findAll();
	}

	@GetMapping("/cliente/{clienteId}")
	public List<FacturaCliente> listarPorCliente(@PathVariable Long clienteId) {
		return facturaService.findByCliente(clienteId);
	}

	@PostMapping("/generar/{clienteId}/{empresa}")
	public ResponseEntity<?> generar(@PathVariable Long clienteId, @PathVariable String empresa) {
		FacturaCliente factura = facturaService.generarFactura(clienteId, empresa);

		if (factura == null) {
			return ResponseEntity.badRequest().body("No hay servicios sin factura para este cliente.");
		}

		return ResponseEntity.ok(factura);
	}

	@PutMapping("/pagar/{facturaId}")
	public FacturaCliente pagar(@PathVariable Long facturaId) {
		return facturaService.marcarComoPagada(facturaId);
	}

	@GetMapping("/empresa/{empresa}")
	public List<FacturaCliente> listarPorEmpresa(@PathVariable String empresa) {
		return facturaService.findByEmpresa(empresa);
	}
}

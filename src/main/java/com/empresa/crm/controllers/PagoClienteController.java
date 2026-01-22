package com.empresa.crm.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.empresa.crm.dto.PagoClienteCreateRequest;
import com.empresa.crm.entities.PagoCliente;
import com.empresa.crm.services.PagoClienteService;

@RestController
@RequestMapping("/api/pagos")
@CrossOrigin(origins = "http://localhost:4200")
public class PagoClienteController {

	private final PagoClienteService service;

	public PagoClienteController(PagoClienteService service) {
		this.service = service;
	}

	@GetMapping("/cliente/{clienteId}")
	public List<PagoCliente> listarPorCliente(@PathVariable Long clienteId) {
		return service.listarPorCliente(clienteId);
	}

	@PostMapping("/cliente/{clienteId}")
	public PagoCliente crear(@PathVariable Long clienteId, @RequestBody PagoClienteCreateRequest req) {
		return service.crearPago(clienteId, req);
	}

	@DeleteMapping("/{pagoId}")
	public void eliminar(@PathVariable Long pagoId) {
		service.eliminarPago(pagoId);
	}
}

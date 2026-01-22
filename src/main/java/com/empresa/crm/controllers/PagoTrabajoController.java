package com.empresa.crm.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.empresa.crm.dto.PagoTrabajoRequest;
import com.empresa.crm.entities.PagoTrabajo;
import com.empresa.crm.services.PagoTrabajoService;

@RestController
@RequestMapping("/api/pagos")
@CrossOrigin(origins = "http://localhost:4200")
public class PagoTrabajoController {

	private final PagoTrabajoService service;

	public PagoTrabajoController(PagoTrabajoService service) {
		this.service = service;
	}

	@GetMapping
	public List<PagoTrabajo> listar(@RequestParam Long clienteId) {
		return service.listarPorCliente(clienteId);
	}

	@GetMapping("/trabajo/{trabajoId}")
	public List<PagoTrabajo> listarPorTrabajo(@PathVariable Long trabajoId) {
		return service.listarPorTrabajo(trabajoId);
	}

	@PostMapping("/trabajo/{trabajoId}")
	public PagoTrabajo pagar(@PathVariable Long trabajoId, @RequestBody PagoTrabajoRequest req) {
		return service.registrarPago(trabajoId, req);
	}
}

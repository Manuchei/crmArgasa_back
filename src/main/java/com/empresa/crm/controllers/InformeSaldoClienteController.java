package com.empresa.crm.controllers;

import java.time.LocalDate;
import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.empresa.crm.dto.HistorialSaldoResponseDTO;
import com.empresa.crm.dto.HistorialTContableResponseDTO;
import com.empresa.crm.services.InformeSaldoClienteService;

@RestController
@RequestMapping("/api/informes/saldos")
@CrossOrigin(origins = "http://localhost:4200")
public class InformeSaldoClienteController {

	private final InformeSaldoClienteService service;

	public InformeSaldoClienteController(InformeSaldoClienteService service) {
		this.service = service;
	}

	@GetMapping("/cliente/{clienteId}")
	public HistorialSaldoResponseDTO obtenerHistorial(@PathVariable Long clienteId) {
		return service.obtenerHistorialSaldo(clienteId);
	}

	@GetMapping("/cliente/{clienteId}/t-contable")
	public HistorialTContableResponseDTO obtenerHistorialTContable(@PathVariable Long clienteId) {
		return service.obtenerHistorialTContable(clienteId);
	}

	@GetMapping
	public List<HistorialSaldoResponseDTO> obtenerHistorialFiltrado(@RequestParam(required = false) Long clienteId,
			@RequestParam(required = false) LocalDate fechaInicio, @RequestParam(required = false) LocalDate fechaFin,
			@RequestParam String empresa) {
		return service.obtenerHistorialSaldoFiltrado(clienteId, fechaInicio, fechaFin, empresa);
	}
}
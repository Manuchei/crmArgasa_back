package com.empresa.crm.controllers;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.empresa.crm.caja.TicketCobroService;

@RestController
@RequestMapping("/api/tickets")
@PreAuthorize("hasAnyRole('USER','ADMIN','DEVELOPER')")
public class TicketCobroController {

	private final TicketCobroService service;

	public TicketCobroController(TicketCobroService service) {
		this.service = service;
	}

	@PostMapping("/cobrar")
	public TicketCobroService.Resultado cobrar(@RequestBody TicketCobroService.Solicitud request) {

		return service.cobrar(request);
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<Map<String, String>> error(ResponseStatusException exception) {

		return ResponseEntity.status(exception.getStatusCode()).body(Map.of("message",
				exception.getReason() == null ? "No se ha podido cobrar el ticket" : exception.getReason()));
	}
}
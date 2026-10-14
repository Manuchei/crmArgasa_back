package com.empresa.crm.caja;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/caja")
@PreAuthorize("hasAnyRole('USER','ADMIN','DEVELOPER')")
public class CajaController {

	private final CajaService service;

	public CajaController(CajaService service) {
		this.service = service;
	}

	public record Cerrar(BigDecimal efectivoContado, String observaciones) {
	}

	@GetMapping("/actual")
	public CajaService.Resumen actual() {
		return service.actual();
	}

	@PostMapping("/abrir")
	public CajaService.Detalle abrir() {
		return service.abrir();
	}

	@PostMapping("/{id}/cerrar")
	public CajaService.Detalle cerrar(@PathVariable Long id, @RequestBody Cerrar request) {
		return service.cerrar(id, request.efectivoContado(), request.observaciones());
	}

	@GetMapping("/{id}")
	public CajaService.Detalle ver(@PathVariable Long id) {
		return service.ver(id);
	}

	@GetMapping
	public CajaService.Historial historial(@RequestParam LocalDate desde, @RequestParam LocalDate hasta,
			@RequestParam(defaultValue = "0") int pagina) {
		return service.historial(desde, hasta, pagina);
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<Map<String, String>> error(ResponseStatusException exception) {
		return ResponseEntity.status(exception.getStatusCode()).body(
				Map.of("message", exception.getReason() == null ? "Operación no permitida" : exception.getReason()));
	}
}
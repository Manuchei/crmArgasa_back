package com.empresa.crm.controllers;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.empresa.crm.services.TicketNumeracionService;
import com.empresa.crm.tenant.TenantContext;

@RestController
@RequestMapping("/api/tickets")
public class TicketNumeracionController {

	private final TicketNumeracionService numeracion;

	public TicketNumeracionController(TicketNumeracionService numeracion) {
		this.numeracion = numeracion;
	}

	public record NumeracionRequest(UUID solicitud) {
	}

	@PostMapping("/numeracion")
	public Map<String, String> reservar(@RequestBody NumeracionRequest request, Authentication authentication) {

		boolean permitido = authentication != null && authentication.isAuthenticated()
				&& authentication.getAuthorities().stream().map(a -> a.getAuthority().replaceFirst("^ROLE_", ""))
						.anyMatch(rol -> rol.equals("USER") || rol.equals("ADMIN") || rol.equals("DEVELOPER"));

		if (!permitido) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}

		String empresa = TenantContext.get();

		if (!"ARGASA".equals(empresa) && !"ELECTROLUGA".equals(empresa)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empresa no válida");
		}

		if (request == null || request.solicitud() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta la solicitud");
		}

		return Map.of("numero", numeracion.reservar(empresa, request.solicitud()));
	}
}
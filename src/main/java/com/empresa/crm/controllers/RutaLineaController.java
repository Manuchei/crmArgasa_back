package com.empresa.crm.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.empresa.crm.entities.RutaLinea;
import com.empresa.crm.services.RutaLineaService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ruta-lineas")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class RutaLineaController {

	private final RutaLineaService rutaLineaService;

	// listar líneas de una ruta
	@GetMapping("/ruta/{rutaId}")
	public List<RutaLinea> listarPorRuta(@PathVariable Long rutaId) {
		return rutaLineaService.findByRuta(rutaId);
	}

	// confirmar entrega de una línea
	@PutMapping("/{id}/confirmar")
	public void confirmar(@PathVariable Long id) {
		rutaLineaService.confirmarEntrega(id);
	}
}

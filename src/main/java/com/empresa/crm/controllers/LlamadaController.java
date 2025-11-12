package com.empresa.crm.controllers;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.empresa.crm.dto.EventoCalendarioDTO;
import com.empresa.crm.entities.Llamada;
import com.empresa.crm.services.LlamadaService;

@RestController
@RequestMapping("/api/llamadas")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class LlamadaController {

	private final LlamadaService llamadaService;

	public LlamadaController(LlamadaService llamadaService) {
		this.llamadaService = llamadaService;
	}

	@GetMapping
	public List<Llamada> listarTodas() {
		return llamadaService.findAll();
	}

	@GetMapping("/{id}")
	public Llamada obtenerPorId(@PathVariable Long id) {
		return llamadaService.findById(id);
	}

	@PostMapping
	public Llamada crear(@RequestBody Llamada llamada) {
		return llamadaService.save(llamada);
	}

	@PutMapping("/{id}")
	public Llamada actualizar(@PathVariable Long id, @RequestBody Llamada llamada) {
		llamada.setId(id);
		return llamadaService.save(llamada);
	}

	@DeleteMapping("/{id}")
	public void eliminar(@PathVariable Long id) {
		llamadaService.deleteById(id);
	}

	@GetMapping("/estado/{estado}")
	public List<Llamada> filtrarPorEstado(@PathVariable String estado) {
		return llamadaService.findByEstado(estado);
	}

	@GetMapping("/rango")
	public List<Llamada> filtrarPorFecha(@RequestParam String inicio, @RequestParam String fin) {
		LocalDateTime fechaInicio = LocalDateTime.parse(inicio);
		LocalDateTime fechaFin = LocalDateTime.parse(fin);
		return llamadaService.findByFechaHoraBetween(fechaInicio, fechaFin);
	}

	@GetMapping("/calendario")
	public List<EventoCalendarioDTO> obtenerEventosCalendario() {
		return llamadaService.getEventosCalendario();
	}

}

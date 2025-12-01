package com.empresa.crm.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.empresa.crm.dto.EventoCalendarioDTO;
import com.empresa.crm.entities.Llamada;
import com.empresa.crm.services.LlamadaService;

@RestController
@RequestMapping("/api/llamadas")
@CrossOrigin("*")
public class LlamadaController {

	private final LlamadaService service;

	public LlamadaController(LlamadaService service) {
		this.service = service;
	}

	@GetMapping
	public List<Llamada> getAll() {
		return service.findAll();
	}

	@GetMapping("/eventos")
	public List<EventoCalendarioDTO> eventos() {
		return service.getEventosCalendario();
	}

	@PostMapping
	public Llamada create(@RequestBody Llamada llamada) {
	    if (llamada.getFecha() == null) {
	        throw new RuntimeException("La fecha está llegando NULL desde Angular");
	    }
	    return service.save(llamada);
	}

	@PutMapping("/{id}")
	public Llamada update(@PathVariable Long id, @RequestBody Llamada llamada) {
	    if (llamada.getFecha() == null) {
	        throw new RuntimeException("La fecha está llegando NULL desde Angular");
	    }
	    llamada.setId(id);
	    return service.save(llamada);
	}


	@DeleteMapping("/{id}")
	public void delete(@PathVariable Long id) {
		service.deleteById(id);
	}
}

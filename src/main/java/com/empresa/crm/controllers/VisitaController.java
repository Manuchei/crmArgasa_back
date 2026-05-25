package com.empresa.crm.controllers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import com.empresa.crm.dto.VisitaRequestDTO;
import com.empresa.crm.entities.Visita;
import com.empresa.crm.services.VisitaService;
import com.empresa.crm.tenant.TenantContext;

@RestController
@RequestMapping("/api/visitas")
public class VisitaController {

	private final VisitaService service;

	public VisitaController(VisitaService service) {
		this.service = service;
	}

	private static void validarEmpresa(String empresa) {
		if (empresa == null || empresa.isBlank()) {
			throw new IllegalArgumentException("Empresa obligatoria");
		}

		String e = empresa.trim().toUpperCase();

		if (!"ARGASA".equals(e) && !"ELECTROLUGA".equals(e)) {
			throw new IllegalArgumentException("Empresa inválida: " + empresa);
		}
	}

	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public List<Visita> getAll(@RequestParam String empresa) {
		validarEmpresa(empresa);
		service.pasarPendientesVencidasAHoy(empresa);
		return service.findAllByEmpresa(empresa);
	}

	@GetMapping(value = "/dia/{fecha}", produces = MediaType.APPLICATION_JSON_VALUE)
	public List<Visita> getVisitasDia(@PathVariable String fecha, @RequestParam String empresa) {
		validarEmpresa(empresa);

		String e = empresa.trim().toUpperCase();

		try {
			TenantContext.set(e);

			LocalDate dia = LocalDate.parse(fecha);
			return service.findByFechaAndEmpresa(dia, e);

		} finally {
			TenantContext.clear();
		}
	}

	@GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public Visita getById(@PathVariable Long id) {
		return service.findById(id);
	}

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public Visita create(@RequestBody VisitaRequestDTO dto) {
		validarDto(dto);

		DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

		Visita visita = new Visita();
		visita.setEmpresa(dto.getEmpresa().trim().toUpperCase());
		visita.setTitulo(dto.getTitulo());
		visita.setFecha(LocalDateTime.parse(dto.getFecha(), f));
		visita.setEstado(dto.getEstado());
		visita.setObservaciones(dto.getObservaciones());

		return service.save(visita);
	}

	@PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public Visita update(@PathVariable Long id, @RequestBody VisitaRequestDTO dto) {
		validarDto(dto);

		Visita visita = service.findById(id);

		if (visita == null) {
			throw new RuntimeException("Visita no encontrada con id " + id);
		}

		String empDto = dto.getEmpresa().trim().toUpperCase();

		if (!empDto.equalsIgnoreCase(visita.getEmpresa())) {
			throw new RuntimeException("No autorizado para editar esta visita");
		}

		DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

		visita.setTitulo(dto.getTitulo());
		visita.setFecha(LocalDateTime.parse(dto.getFecha(), f));
		visita.setEstado(dto.getEstado());
		visita.setObservaciones(dto.getObservaciones());
		visita.setEmpresa(empDto);

		return service.save(visita);
	}

	@DeleteMapping("/{id}")
	public void delete(@PathVariable Long id, @RequestParam String empresa) {
		validarEmpresa(empresa);

		Visita visita = service.findById(id);

		if (visita == null)
			return;

		if (!visita.getEmpresa().equalsIgnoreCase(empresa.trim().toUpperCase())) {
			throw new RuntimeException("No autorizado para borrar esta visita");
		}

		service.deleteById(id);
	}

	private void validarDto(VisitaRequestDTO dto) {
		validarEmpresa(dto.getEmpresa());

		if (dto.getTitulo() == null || dto.getTitulo().trim().isEmpty()) {
			throw new IllegalArgumentException("El título no puede estar vacío");
		}

		if (dto.getFecha() == null || dto.getFecha().trim().isEmpty()) {
			throw new IllegalArgumentException("La fecha no puede estar vacía");
		}

		if (dto.getEstado() == null || dto.getEstado().trim().isEmpty()) {
			dto.setEstado("pendiente");
		}
	}
}
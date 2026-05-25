package com.empresa.crm.controllers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import com.empresa.crm.dto.TareaRequestDTO;
import com.empresa.crm.entities.Tarea;
import com.empresa.crm.services.TareaService;
import com.empresa.crm.tenant.TenantContext;

@RestController
@RequestMapping("/api/tareas")
public class TareaController {

	private final TareaService service;

	public TareaController(TareaService service) {
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
	public List<Tarea> getAll(@RequestParam String empresa) {
		validarEmpresa(empresa);
		service.pasarPendientesVencidasAHoy(empresa);
		return service.findAllByEmpresa(empresa);
	}

	@GetMapping(value = "/dia/{fecha}", produces = MediaType.APPLICATION_JSON_VALUE)
	public List<Tarea> getTareasDia(@PathVariable String fecha, @RequestParam String empresa) {
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
	public Tarea getById(@PathVariable Long id) {
		return service.findById(id);
	}

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public Tarea create(@RequestBody TareaRequestDTO dto) {
		validarDto(dto);

		DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

		Tarea tarea = new Tarea();
		tarea.setEmpresa(dto.getEmpresa().trim().toUpperCase());
		tarea.setTitulo(dto.getTitulo());
		tarea.setFecha(LocalDateTime.parse(dto.getFecha(), f));
		tarea.setEstado(dto.getEstado());
		tarea.setObservaciones(dto.getObservaciones());

		return service.save(tarea);
	}

	@PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public Tarea update(@PathVariable Long id, @RequestBody TareaRequestDTO dto) {
		validarDto(dto);

		Tarea tarea = service.findById(id);

		if (tarea == null) {
			throw new RuntimeException("Tarea no encontrada con id " + id);
		}

		String empDto = dto.getEmpresa().trim().toUpperCase();

		if (!empDto.equalsIgnoreCase(tarea.getEmpresa())) {
			throw new RuntimeException("No autorizado para editar esta tarea");
		}

		DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

		tarea.setTitulo(dto.getTitulo());
		tarea.setFecha(LocalDateTime.parse(dto.getFecha(), f));
		tarea.setEstado(dto.getEstado());
		tarea.setObservaciones(dto.getObservaciones());
		tarea.setEmpresa(empDto);

		return service.save(tarea);
	}

	@DeleteMapping("/{id}")
	public void delete(@PathVariable Long id, @RequestParam String empresa) {
		validarEmpresa(empresa);

		Tarea tarea = service.findById(id);

		if (tarea == null)
			return;

		if (!tarea.getEmpresa().equalsIgnoreCase(empresa.trim().toUpperCase())) {
			throw new RuntimeException("No autorizado para borrar esta tarea");
		}

		service.deleteById(id);
	}

	private void validarDto(TareaRequestDTO dto) {
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
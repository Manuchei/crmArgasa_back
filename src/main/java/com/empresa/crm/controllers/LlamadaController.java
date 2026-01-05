package com.empresa.crm.controllers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import com.empresa.crm.dto.EventoCalendarioDTO;
import com.empresa.crm.dto.LlamadaRequestDTO;
import com.empresa.crm.entities.Llamada;
import com.empresa.crm.services.LlamadaService;

@RestController
@RequestMapping("/api/llamadas")
@CrossOrigin(origins = "*")
public class LlamadaController {

    private final LlamadaService service;

    public LlamadaController(LlamadaService service) {
        this.service = service;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Llamada> getAll() {
        return service.findAll();
    }

    @GetMapping(value = "/dia/{fecha}", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Llamada> getLlamadasDia(@PathVariable String fecha) {
        LocalDate dia = LocalDate.parse(fecha); // yyyy-MM-dd
        return service.findByFecha(dia);
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Llamada getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping(value = "/eventos", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<EventoCalendarioDTO> eventos() {
        return service.getEventosCalendario();
    }

    // ✅ POST con DTO (evita 415 por clienteId/Cliente)
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Llamada create(@RequestBody LlamadaRequestDTO dto) {
        validarDto(dto);

        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        LocalDateTime fecha = LocalDateTime.parse(dto.getFecha(), f);

        Llamada llamada = new Llamada();
        llamada.setMotivo(dto.getMotivo());
        llamada.setFecha(fecha);
        llamada.setEstado(dto.getEstado());
        llamada.setObservaciones(dto.getObservaciones());

        // ✅ clienteId: lo ignoramos por ahora (evita errores).
        // Si luego quieres asociarlo, lo hacemos con ClienteRepository.

        return service.save(llamada);
    }

    // ✅ PUT con DTO
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Llamada update(@PathVariable Long id, @RequestBody LlamadaRequestDTO dto) {
        validarDto(dto);

        Llamada llamada = service.findById(id);
        if (llamada == null) {
            throw new RuntimeException("Llamada no encontrada con id " + id);
        }

        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        LocalDateTime fecha = LocalDateTime.parse(dto.getFecha(), f);

        llamada.setMotivo(dto.getMotivo());
        llamada.setFecha(fecha);
        llamada.setEstado(dto.getEstado());
        llamada.setObservaciones(dto.getObservaciones());

        return service.save(llamada);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.deleteById(id);
    }

    private void validarDto(LlamadaRequestDTO dto) {
        if (dto.getMotivo() == null || dto.getMotivo().trim().isEmpty()) {
            throw new IllegalArgumentException("El motivo no puede estar vacío");
        }
        if (dto.getFecha() == null || dto.getFecha().trim().isEmpty()) {
            throw new IllegalArgumentException("La fecha no puede estar vacía");
        }
        if (dto.getEstado() == null || dto.getEstado().trim().isEmpty()) {
            dto.setEstado("pendiente");
        }
    }
}

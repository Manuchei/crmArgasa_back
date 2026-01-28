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
import com.empresa.crm.serviceImpl.LlamadaServiceImpl;
import com.empresa.crm.services.LlamadaService;
import com.empresa.crm.tenant.TenantContext;


@RestController
@RequestMapping("/api/llamadas")
public class LlamadaController {

    private final LlamadaService service;
    private final LlamadaServiceImpl serviceImpl; // ✅ para métodos con empresa

    public LlamadaController(LlamadaService service, LlamadaServiceImpl serviceImpl) {
        this.service = service;
        this.serviceImpl = serviceImpl;
    }

    private static void validarEmpresa(String empresa) {
        if (empresa == null || empresa.isBlank()) {
            throw new IllegalArgumentException("Empresa obligatoria (ARGASA / ELECTROLUGA)");
        }
        String e = empresa.trim().toUpperCase();
        if (!"ARGASA".equals(e) && !"ELECTROLUGA".equals(e)) {
            throw new IllegalArgumentException("Empresa inválida: " + empresa + " (ARGASA / ELECTROLUGA)");
        }
    }

    // ✅ Si quieres, úsalo con ?empresa=ARGASA
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Llamada> getAll(@RequestParam(required = false) String empresa) {
        if (empresa == null || empresa.isBlank()) {
            // compatibilidad: devuelve todo
            return service.findAll();
        }
        validarEmpresa(empresa);
        return serviceImpl.findAllByEmpresa(empresa);
    }

    // ✅ Día + empresa obligatoria
    @GetMapping(value = "/dia/{fecha}", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Llamada> getLlamadasDia(@PathVariable String fecha, @RequestParam String empresa) {
        validarEmpresa(empresa);

        String e = empresa.trim().toUpperCase();
        try {
            TenantContext.set(e); // ✅ ASEGURAS que consulta el tenant correcto
            LocalDate dia = LocalDate.parse(fecha); // yyyy-MM-dd
            return serviceImpl.findByFechaAndEmpresa(dia, e);
        } finally {
            TenantContext.clear();
        }
    }


    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Llamada getById(@PathVariable Long id) {
        return service.findById(id);
    }

    // ✅ Eventos por empresa obligatoria
    @GetMapping(value = "/eventos", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<EventoCalendarioDTO> eventos(@RequestParam String empresa) {
        validarEmpresa(empresa);
        return serviceImpl.getEventosCalendarioByEmpresa(empresa);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Llamada create(@RequestBody LlamadaRequestDTO dto) {
        validarDto(dto);

        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        LocalDateTime fecha = LocalDateTime.parse(dto.getFecha(), f);

        Llamada llamada = new Llamada();
        llamada.setEmpresa(dto.getEmpresa().trim().toUpperCase()); // ✅ AQUÍ
        llamada.setMotivo(dto.getMotivo());
        llamada.setFecha(fecha);
        llamada.setEstado(dto.getEstado());
        llamada.setObservaciones(dto.getObservaciones());

        return service.save(llamada);
    }


    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Llamada update(@PathVariable Long id, @RequestBody LlamadaRequestDTO dto) {
        validarDto(dto);

        Llamada llamada = service.findById(id);
        if (llamada == null) throw new RuntimeException("Llamada no encontrada con id " + id);

        // ✅ Seguridad: no mezclar empresas
        String empDto = dto.getEmpresa().trim().toUpperCase();
        if (!empDto.equalsIgnoreCase(llamada.getEmpresa())) {
            throw new RuntimeException("No autorizado para editar esta llamada");
        }

        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        LocalDateTime fecha = LocalDateTime.parse(dto.getFecha(), f);

        llamada.setMotivo(dto.getMotivo());
        llamada.setFecha(fecha);
        llamada.setEstado(dto.getEstado());
        llamada.setObservaciones(dto.getObservaciones());

        // ✅ conservar empresa real
        llamada.setEmpresa(empDto);

        return service.save(llamada);
    }



    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, @RequestParam String empresa) {
        validarEmpresa(empresa);
        Llamada llamada = service.findById(id);
        if (llamada == null) return;

        if (!llamada.getEmpresa().equalsIgnoreCase(empresa.trim().toUpperCase())) {
            throw new RuntimeException("No autorizado para borrar esta llamada");
        }

        service.deleteById(id);
    }


    private void validarDto(LlamadaRequestDTO dto) {
        validarEmpresa(dto.getEmpresa());

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

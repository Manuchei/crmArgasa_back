package com.empresa.crm.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.empresa.crm.dto.EventoCalendarioDTO;
import com.empresa.crm.entities.Llamada;
import com.empresa.crm.repositories.LlamadaRepository;
import com.empresa.crm.services.LlamadaService;
import com.empresa.crm.tenant.TenantContext;

@Service
public class LlamadaServiceImpl implements LlamadaService {

    private final LlamadaRepository repo;

    public LlamadaServiceImpl(LlamadaRepository repo) {
        this.repo = repo;
    }

    // ==========================
    // Validación empresa
    // ==========================
    private static void validarEmpresa(String empresa) {
        if (empresa == null || empresa.isBlank()) {
            throw new IllegalArgumentException("Empresa obligatoria (ARGASA / ELECTROLUGA)");
        }
        String e = empresa.trim().toUpperCase();
        if (!"ARGASA".equals(e) && !"ELECTROLUGA".equals(e)) {
            throw new IllegalArgumentException("Empresa inválida: " + empresa + " (ARGASA / ELECTROLUGA)");
        }
    }

    // ==========================
    // CRUD básico
    // ==========================
    @Override
    public List<Llamada> findAll() {
        return repo.findByEmpresa(TenantContext.get());
    }

    public List<Llamada> findAllByEmpresa(String empresa) {
        validarEmpresa(empresa);
        return repo.findByEmpresa(empresa.trim().toUpperCase());
    }

    @Override
    public Llamada findById(Long id) {
        return repo.findById(id).orElse(null);
    }

    @Override
    public Llamada save(Llamada llamada) {
        validarEmpresa(llamada.getEmpresa());
        llamada.setEmpresa(llamada.getEmpresa().trim().toUpperCase());
        return repo.save(llamada);
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
    }

    // ==========================
    // Búsqueda por día
    // ==========================
    @Override
    public List<Llamada> findByFecha(LocalDate fecha) {
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.plusDays(1).atStartOfDay().minusNanos(1);

        return repo.findByEmpresaAndFechaBetween(
            TenantContext.get(),
            inicio,
            fin
        );
    }

    // ✅ MÉTODO CLAVE (día + empresa)
    public List<Llamada> findByFechaAndEmpresa(LocalDate dia, String empresa) {
        validarEmpresa(empresa);

        LocalDateTime inicio = dia.atStartOfDay();
        LocalDateTime fin = dia.plusDays(1).atStartOfDay().minusNanos(1);

        return repo.findByEmpresaAndFechaBetween(
            empresa.trim().toUpperCase(),
            inicio,
            fin
        );
    }

    // ==========================
    // Eventos calendario
    // ==========================
    public List<EventoCalendarioDTO> getEventosCalendarioByEmpresa(String empresa) {
        validarEmpresa(empresa);
        String emp = empresa.trim().toUpperCase();

        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

        return repo.findByEmpresa(emp).stream()
            .filter(l -> l.getFecha() != null)
            .map(l -> {
                EventoCalendarioDTO dto = new EventoCalendarioDTO();
                dto.setId(l.getId());
                dto.setTitle(l.getMotivo());

                String fechaFormateada = l.getFecha().format(f);
                dto.setStart(fechaFormateada);
                dto.setFecha(fechaFormateada);

                dto.setEstado(l.getEstado());
                dto.setObservaciones(l.getObservaciones());
                return dto;
            })
            .collect(Collectors.toList());
    }

    // Compatibilidad: eventos del tenant actual
    @Override
    public List<EventoCalendarioDTO> getEventosCalendario() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

        return repo.findByEmpresa(TenantContext.get()).stream()
            .filter(l -> l.getFecha() != null)
            .map(l -> {
                EventoCalendarioDTO dto = new EventoCalendarioDTO();
                dto.setId(l.getId());
                dto.setTitle(l.getMotivo());

                String fechaFormateada = l.getFecha().format(f);
                dto.setStart(fechaFormateada);
                dto.setFecha(fechaFormateada);

                dto.setEstado(l.getEstado());
                dto.setObservaciones(l.getObservaciones());
                return dto;
            })
            .collect(Collectors.toList());
    }
    
    public List<Llamada> getProximasPendientesByEmpresa(String empresa, int limit) {
        LocalDateTime ahora = LocalDateTime.now();
        return repo
            .findByEmpresaAndEstadoAndFechaAfterOrderByFechaAsc(
                empresa, "pendiente", ahora, PageRequest.of(0, limit)
            )
            .getContent();
    }

}

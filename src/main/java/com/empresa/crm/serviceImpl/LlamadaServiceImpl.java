package com.empresa.crm.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.empresa.crm.dto.EventoCalendarioDTO;
import com.empresa.crm.entities.Llamada;
import com.empresa.crm.repositories.LlamadaRepository;
import com.empresa.crm.services.LlamadaService;

@Service
public class LlamadaServiceImpl implements LlamadaService {

    private final LlamadaRepository repo;

    public LlamadaServiceImpl(LlamadaRepository repo) {
        this.repo = repo;
    }

    @Override
    public List<Llamada> findAll() {
        return repo.findAll();
    }

    @Override
    public Llamada findById(Long id) {
        return repo.findById(id).orElse(null);
    }

    @Override
    public Llamada save(Llamada llamada) {
        return repo.save(llamada);
    }

    @Override
    public void deleteById(Long id) {
        repo.deleteById(id);
    }

    // ✅ Llamadas por día
    @Override
    public List<Llamada> findByFecha(LocalDate fecha) {
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(23, 59, 59);
        return repo.findByFechaBetween(inicio, fin);
    }

    // ✅ Eventos FullCalendar / Material Calendar
    @Override
    public List<EventoCalendarioDTO> getEventosCalendario() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

        return repo.findAll().stream()
            .filter(l -> l.getFecha() != null) // ✅ evita NPE
            .map(l -> {
                EventoCalendarioDTO dto = new EventoCalendarioDTO();
                dto.setId(l.getId());
                dto.setTitle(l.getMotivo());

                String fechaFormateada = l.getFecha().format(f);

                dto.setStart(fechaFormateada);
                dto.setFecha(fechaFormateada); // opcional

                dto.setEstado(l.getEstado());
                dto.setObservaciones(l.getObservaciones());
                return dto;
            })
            .collect(Collectors.toList());
    }
}

package com.empresa.crm.serviceImpl;

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

	private final LlamadaRepository llamadaRepository;

	public LlamadaServiceImpl(LlamadaRepository llamadaRepository) {
		this.llamadaRepository = llamadaRepository;
	}

	@Override
	public List<Llamada> findAll() {
		return llamadaRepository.findAll();
	}

	@Override
	public Llamada findById(Long id) {
		return llamadaRepository.findById(id).orElse(null);
	}

	@Override
	public Llamada save(Llamada llamada) {
		return llamadaRepository.save(llamada);
	}

	@Override
	public void deleteById(Long id) {
		llamadaRepository.deleteById(id);
	}

	@Override
	public List<Llamada> findByEstado(String estado) {
		return llamadaRepository.findByEstado(estado);
	}

	@Override
	public List<Llamada> findByFechaHoraBetween(LocalDateTime inicio, LocalDateTime fin) {
		return llamadaRepository.findByFechaHoraBetween(inicio, fin);
	}

	@Override
	public List<EventoCalendarioDTO> getEventosCalendario() {
	    return llamadaRepository.findAll().stream()
	        .map(llamada -> {
	            EventoCalendarioDTO dto = new EventoCalendarioDTO();
	            dto.setId(llamada.getId());
	            dto.setTitle(llamada.getMotivo()); // 👈 o añade " - " + estado si quieres
	            dto.setStart(llamada.getFechaHora());
	            dto.setEstado(llamada.getEstado());
	            dto.setMotivo(llamada.getMotivo());
	            dto.setObservaciones(llamada.getObservaciones());
	            return dto;
	        })
	        .collect(Collectors.toList());
	}


}
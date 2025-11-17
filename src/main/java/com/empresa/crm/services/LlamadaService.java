package com.empresa.crm.services;

import java.time.LocalDateTime;
import java.util.List;

import com.empresa.crm.dto.EventoCalendarioDTO;
import com.empresa.crm.entities.Llamada;

public interface LlamadaService {
	List<Llamada> findAll();

	Llamada findById(Long id);

	Llamada save(Llamada llamada);

	void deleteById(Long id);

	List<EventoCalendarioDTO> getEventosCalendario();
}

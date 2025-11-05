package com.empresa.crm.serviceImpl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
		List<Llamada> llamadas = llamadaRepository.findAll();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

		return llamadas.stream().map(llamada -> {
			String clienteNombre = (llamada.getCliente() != null) ? llamada.getCliente().getNombre() : "Sin cliente";
			String title = llamada.getMotivo() + " - " + clienteNombre;
			String start = llamada.getFechaHora().format(formatter);
			String end = llamada.getFechaHora().plusMinutes(30).format(formatter); // duración estimada 30 min
			return new EventoCalendarioDTO(title, start, end, llamada.getEstado());
		}).toList();
	}
}
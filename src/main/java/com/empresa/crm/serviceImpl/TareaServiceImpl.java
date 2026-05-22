package com.empresa.crm.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.empresa.crm.entities.Tarea;
import com.empresa.crm.repositories.TareaRepository;
import com.empresa.crm.services.TareaService;

@Service
public class TareaServiceImpl implements TareaService {

	private final TareaRepository repo;

	public TareaServiceImpl(TareaRepository repo) {
		this.repo = repo;
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

	@Override
	public List<Tarea> findAllByEmpresa(String empresa) {
		validarEmpresa(empresa);
		return repo.findByEmpresa(empresa.trim().toUpperCase());
	}

	@Override
	public List<Tarea> findByFechaAndEmpresa(LocalDate fecha, String empresa) {
		validarEmpresa(empresa);

		LocalDateTime inicio = fecha.atStartOfDay();
		LocalDateTime fin = fecha.plusDays(1).atStartOfDay().minusNanos(1);

		return repo.findByEmpresaAndFechaBetween(empresa.trim().toUpperCase(), inicio, fin);
	}

	@Override
	public Tarea findById(Long id) {
		return repo.findById(id).orElse(null);
	}

	@Override
	public Tarea save(Tarea tarea) {
		validarEmpresa(tarea.getEmpresa());

		tarea.setEmpresa(tarea.getEmpresa().trim().toUpperCase());

		if (tarea.getEstado() == null || tarea.getEstado().isBlank()) {
			tarea.setEstado("pendiente");
		}

		return repo.save(tarea);
	}

	@Override
	public void deleteById(Long id) {
		repo.deleteById(id);
	}

	@Override
	public void pasarPendientesVencidasAHoy(String empresa) {
		validarEmpresa(empresa);

		String emp = empresa.trim().toUpperCase();
		LocalDateTime hoyInicio = LocalDate.now().atStartOfDay();

		List<Tarea> vencidas = repo.findByEmpresaAndEstadoAndFechaBefore(emp, "pendiente", hoyInicio);

		LocalDate hoy = LocalDate.now();

		for (Tarea t : vencidas) {
			t.setFecha(LocalDateTime.of(hoy, t.getFecha().toLocalTime()));
		}

		repo.saveAll(vencidas);
	}
}
package com.empresa.crm.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.empresa.crm.entities.Visita;
import com.empresa.crm.repositories.VisitaRepository;
import com.empresa.crm.services.VisitaService;

@Service
public class VisitaServiceImpl implements VisitaService {

	private final VisitaRepository repo;

	public VisitaServiceImpl(VisitaRepository repo) {
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
	public List<Visita> findAllByEmpresa(String empresa) {
		validarEmpresa(empresa);
		return repo.findByEmpresa(empresa.trim().toUpperCase());
	}

	@Override
	public List<Visita> findByFechaAndEmpresa(LocalDate fecha, String empresa) {
		validarEmpresa(empresa);

		LocalDateTime fin = fecha.plusDays(1).atStartOfDay().minusNanos(1);

		return repo.findByEmpresaAndEstadoInAndFechaLessThanEqualOrderByFechaAsc(empresa.trim().toUpperCase(),
				List.of("pendiente", "en_progreso"), fin);
	}

	@Override
	public Visita findById(Long id) {
		return repo.findById(id).orElse(null);
	}

	@Override
	public Visita save(Visita visita) {
		validarEmpresa(visita.getEmpresa());

		visita.setEmpresa(visita.getEmpresa().trim().toUpperCase());

		if (visita.getEstado() == null || visita.getEstado().isBlank()) {
			visita.setEstado("pendiente");
		}

		return repo.save(visita);
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

		List<Visita> vencidas = repo.findByEmpresaAndEstadoAndFechaBefore(emp, "pendiente", hoyInicio);

		LocalDate hoy = LocalDate.now();

		for (Visita v : vencidas) {
			v.setFecha(LocalDateTime.of(hoy, v.getFecha().toLocalTime()));
		}

		repo.saveAll(vencidas);
	}
}
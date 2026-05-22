package com.empresa.crm.services;

import java.time.LocalDate;
import java.util.List;

import com.empresa.crm.entities.Visita;

public interface VisitaService {

	List<Visita> findAllByEmpresa(String empresa);

	List<Visita> findByFechaAndEmpresa(LocalDate fecha, String empresa);

	Visita findById(Long id);

	Visita save(Visita visita);

	void deleteById(Long id);

	void pasarPendientesVencidasAHoy(String empresa);
}
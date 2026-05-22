package com.empresa.crm.services;

import java.time.LocalDate;
import java.util.List;

import com.empresa.crm.entities.Tarea;

public interface TareaService {

	List<Tarea> findAllByEmpresa(String empresa);

	List<Tarea> findByFechaAndEmpresa(LocalDate fecha, String empresa);

	Tarea findById(Long id);

	Tarea save(Tarea tarea);

	void deleteById(Long id);

	void pasarPendientesVencidasAHoy(String empresa);
}
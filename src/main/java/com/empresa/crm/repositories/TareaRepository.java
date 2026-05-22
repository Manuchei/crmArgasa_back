package com.empresa.crm.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.empresa.crm.entities.Tarea;

public interface TareaRepository extends JpaRepository<Tarea, Long> {

	List<Tarea> findByEmpresa(String empresa);

	List<Tarea> findByEmpresaAndFechaBetween(String empresa, LocalDateTime inicio, LocalDateTime fin);

	List<Tarea> findByEmpresaAndEstadoAndFechaBefore(String empresa, String estado, LocalDateTime fecha);
}
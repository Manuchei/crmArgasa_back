package com.empresa.crm.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.empresa.crm.entities.Visita;

public interface VisitaRepository extends JpaRepository<Visita, Long> {

	List<Visita> findByEmpresa(String empresa);

	List<Visita> findByEmpresaAndFechaBetween(String empresa, LocalDateTime inicio, LocalDateTime fin);

	List<Visita> findByEmpresaAndEstadoAndFechaBefore(String empresa, String estado, LocalDateTime fecha);

	List<Visita> findByEmpresaAndEstadoInAndFechaLessThanEqualOrderByFechaAsc(String empresa, List<String> estados,
			LocalDateTime fecha);
}
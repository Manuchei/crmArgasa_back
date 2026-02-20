package com.empresa.crm.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.empresa.crm.entities.Llamada;

public interface LlamadaRepository extends JpaRepository<Llamada, Long> {
	List<Llamada> findByFechaBetween(LocalDateTime inicio, LocalDateTime fin);

	List<Llamada> findByEstado(String estado);

	Page<Llamada> findByFechaAfterOrderByFechaAsc(LocalDateTime fecha, Pageable pageable);

	// ✅ Día + empresa
	List<Llamada> findByEmpresaAndFechaBetween(String empresa, LocalDateTime inicio, LocalDateTime fin);

	// ✅ Todos por empresa
	List<Llamada> findByEmpresa(String empresa);

	List<Llamada> findByEmpresaAndEstado(String empresa, String estado);

	Page<Llamada> findByEmpresaAndFechaAfterOrderByFechaAsc(String empresa, LocalDateTime fecha, Pageable pageable);

	Page<Llamada> findByEmpresaAndEstadoAndFechaAfterOrderByFechaAsc(String empresa, String estado, LocalDateTime fecha,
			Pageable pageable);

}

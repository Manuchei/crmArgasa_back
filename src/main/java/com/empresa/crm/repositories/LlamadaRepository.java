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



}

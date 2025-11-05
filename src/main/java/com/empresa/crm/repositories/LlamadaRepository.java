package com.empresa.crm.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.empresa.crm.entities.Llamada;

@Repository
public interface LlamadaRepository extends JpaRepository<Llamada, Long> {

	List<Llamada> findByEstado(String estado);

	List<Llamada> findByFechaHoraBetween(LocalDateTime inicio, LocalDateTime fin);
}
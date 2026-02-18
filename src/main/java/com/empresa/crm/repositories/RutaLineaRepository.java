package com.empresa.crm.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.empresa.crm.entities.RutaLinea;

public interface RutaLineaRepository extends JpaRepository<RutaLinea, Long> {

	List<RutaLinea> findByRutaId(Long rutaId);

}

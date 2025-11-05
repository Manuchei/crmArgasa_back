package com.empresa.crm.repositories;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.empresa.crm.entities.Ruta;

@Repository
public interface RutaRepository extends JpaRepository<Ruta, Long> {

    List<Ruta> findByEstado(String estado);

	List<Ruta> findByNombreTransportistaContainingIgnoreCase(String nombre);

    List<Ruta> findByFecha(LocalDate fecha);
}
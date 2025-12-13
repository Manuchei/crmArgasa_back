package com.empresa.crm.repositories;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.empresa.crm.entities.Ruta;

@Repository
public interface RutaRepository extends JpaRepository<Ruta, Long> {

	List<Ruta> findByEstado(String estado);

	List<Ruta> findByNombreTransportistaContainingIgnoreCase(String nombre);

	List<Ruta> findByFecha(LocalDate fecha);

	List<Ruta> findByFechaAndNombreTransportistaIgnoreCase(LocalDate fecha, String nombre);

	@Query("select distinct r.nombreTransportista from Ruta r where r.fecha = :fecha")
	List<String> findTransportistasByFecha(@Param("fecha") LocalDate fecha);

	List<Ruta> findByFechaAndNombreTransportistaContainingIgnoreCase(LocalDate fecha, String nombre);
}

package com.empresa.crm.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.empresa.crm.entities.Transportista;

public interface TransportistaRepository extends JpaRepository<Transportista, Long> {
	Optional<Transportista> findByNombre(String nombre);
}

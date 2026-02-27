package com.empresa.crm.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.empresa.crm.entities.ClienteProducto;
import com.empresa.crm.entities.RutaLinea;

public interface RutaLineaRepository extends JpaRepository<RutaLinea, Long> {

	List<RutaLinea> findByRutaId(Long rutaId);

	// RutaLineaRepository
	List<RutaLinea> findByRutaIdAndRutaEmpresa(Long rutaId, String empresa);

	// ✅ Obtener una línea por id validando empresa
	Optional<RutaLinea> findByIdAndRutaEmpresa(Long id, String empresa);
}

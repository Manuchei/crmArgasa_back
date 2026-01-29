package com.empresa.crm.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.empresa.crm.entities.LineaAlbaranCliente;
import org.springframework.data.repository.query.Param;


@Repository
public interface LineaAlbaranClienteRepository extends JpaRepository<LineaAlbaranCliente, Long> {
	
	@Query("""
			SELECT l
			FROM LineaAlbaranCliente l
			JOIN l.albaran a
			WHERE a.cliente.id = :clienteId
			AND l.empresa = :empresa
			AND	a.empresa = :empresa
			AND a.confirmado = true
			AND l.facturaV2Id IS NULL 
			""")
	
	List<LineaAlbaranCliente> pendientesPorCliente(@Param("clienteId") Long clienteId,
			@Param("empresa")String empresa);
	
	@Query("""
			  SELECT l
			  FROM LineaAlbaranCliente l
			  JOIN l.albaran a
			  WHERE l.id IN :ids
			    AND l.empresa = :empresa
			    AND a.empresa = :empresa
			    AND a.cliente.id = :clienteId
			    AND a.confirmado = true
			    AND l.facturaV2Id IS NULL
			""")
			List<LineaAlbaranCliente> findPendientesSeleccionadas(@Param("ids") List<Long> ids,
			                                                     @Param("empresa") String empresa,
			                                                     @Param("clienteId") Long clienteId);

	
	
}

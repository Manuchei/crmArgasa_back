package com.empresa.crm.repositories;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;


import com.empresa.crm.entities.FacturaCliente;

@Repository
public interface FacturaClienteRepository extends JpaRepository<FacturaCliente, Long> {
	List<FacturaCliente> findByEmpresa(String empresa);

	List<FacturaCliente> findByPagada(boolean pagada);
	

	Optional<FacturaCliente> findByIdAndEmpresa(Long id, String empresa);
	List<FacturaCliente> findByClienteIdAndEmpresa(Long clienteId, String empresa);
	
	Optional<FacturaCliente> findTopByEmpresaOrderByIdDesc(String empresa);
	
	@Query("""
			SELECT f FROM FacturaCliente f
			WHERE f.empresa = :empresa
			AND (:pagada IS NULL OR f.pagada = :pagada)
			AND (:clienteId IS NULL OR f.cliente.id = :clienteId)
			AND (:desde IS NULL OR f.fechaEmision >= :desde)
			AND (:hasta IS NULL OR f.fechaEmision <= :hasta)
			ORDER BY f.fechaEmision DESC
			""")
			List<FacturaCliente> buscarInforme(
			        @Param("empresa") String empresa,
			        @Param("pagada") Boolean pagada,
			        @Param("clienteId") Long clienteId,
			        @Param("desde") LocalDate desde,
			        @Param("hasta") LocalDate hasta
			);

}

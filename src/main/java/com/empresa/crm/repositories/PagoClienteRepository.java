package com.empresa.crm.repositories;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.empresa.crm.entities.PagoCliente;

public interface PagoClienteRepository extends JpaRepository<PagoCliente, Long> {

	List<PagoCliente> findByClienteIdOrderByFechaAscIdAsc(Long clienteId);

	List<PagoCliente> findByClienteIdAndFechaBetweenOrderByFechaAscIdAsc(Long clienteId, LocalDate fechaInicio,
			LocalDate fechaFin);

	List<PagoCliente> findByClienteIdAndFechaGreaterThanEqualOrderByFechaAscIdAsc(Long clienteId,
			LocalDate fechaInicio);

	List<PagoCliente> findByClienteIdAndFechaLessThanEqualOrderByFechaAscIdAsc(Long clienteId, LocalDate fechaFin);

	@Query("""
			select coalesce(sum(p.importe), 0)
			from PagoCliente p
			where p.facturaV2Id = :facturaId
			  and p.empresa = :empresa
			""")
	Double sumarPagosFactura(@Param("facturaId") Long facturaId, @Param("empresa") String empresa);
}
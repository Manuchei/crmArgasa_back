package com.empresa.crm.repositories;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.empresa.crm.entities.PagoCliente;
import com.empresa.crm.entities.Trabajo;

public interface PagoClienteRepository extends JpaRepository<PagoCliente, Long> {

	List<PagoCliente> findByClienteIdOrderByFechaAscIdAsc(Long clienteId);
	
	List<PagoCliente> findByClienteIdAndFechaBetweenOrderByFechaAscIdAsc(Long clienteId, LocalDate fechaInicio, LocalDate fechaFin);

	List<PagoCliente> findByClienteIdAndFechaGreaterThanEqualOrderByFechaAscIdAsc(Long clienteId, LocalDate fechaInicio);

	List<PagoCliente> findByClienteIdAndFechaLessThanEqualOrderByFechaAscIdAsc(Long clienteId, LocalDate fechaFin);
	
	

}
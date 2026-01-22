package com.empresa.crm.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.empresa.crm.entities.PagoCliente;

public interface PagoClienteRepository extends JpaRepository<PagoCliente, Long> {

	List<PagoCliente> findByClienteIdOrderByFechaAscIdAsc(Long clienteId);
}

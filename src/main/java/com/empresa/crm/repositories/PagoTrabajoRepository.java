package com.empresa.crm.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.empresa.crm.entities.PagoTrabajo;

public interface PagoTrabajoRepository extends JpaRepository<PagoTrabajo, Long> {

	List<PagoTrabajo> findByClienteIdOrderByFechaDesc(Long clienteId);

	List<PagoTrabajo> findByTrabajoIdOrderByFechaDesc(Long trabajoId);
}

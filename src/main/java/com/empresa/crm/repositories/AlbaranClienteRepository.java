package com.empresa.crm.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.empresa.crm.entities.AlbaranCliente;

public interface AlbaranClienteRepository extends JpaRepository<AlbaranCliente, Long> {

	List<AlbaranCliente> findByEmpresaOrderByFechaEmisionDescIdDesc(String empresa);

	List<AlbaranCliente> findByClienteIdOrderByFechaEmisionDescIdDesc(Long clienteId);
}

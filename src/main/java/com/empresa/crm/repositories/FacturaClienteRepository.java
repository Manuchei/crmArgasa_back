package com.empresa.crm.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.empresa.crm.entities.FacturaCliente;

@Repository
public interface FacturaClienteRepository extends JpaRepository<FacturaCliente, Long> {
	List<FacturaCliente> findByEmpresa(String empresa);

	List<FacturaCliente> findByPagada(boolean pagada);
}

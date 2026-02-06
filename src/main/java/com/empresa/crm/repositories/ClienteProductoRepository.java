package com.empresa.crm.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.empresa.crm.entities.ClienteProducto;

public interface ClienteProductoRepository extends JpaRepository<ClienteProducto, Long> {
	
	List<ClienteProducto> findByClienteId(Long clienteId);

}

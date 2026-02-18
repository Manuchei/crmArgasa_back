package com.empresa.crm.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.empresa.crm.entities.ClienteProducto;

public interface ClienteProductoRepository extends JpaRepository<ClienteProducto, Long> {

	List<ClienteProducto> findByClienteId(Long clienteId);

	Optional<ClienteProducto> findByClienteIdAndProductoId(Long clienteId, Long productoId);

}

package com.empresa.crm.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.empresa.crm.entities.ServicioCliente;

@Repository
public interface ServicioClienteRepository extends JpaRepository<ServicioCliente, Long> {
    List<ServicioCliente> findByClienteId(Long clienteId);
    List<ServicioCliente> findByPagado(boolean pagado);
}

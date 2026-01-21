package com.empresa.crm.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.empresa.crm.entities.LineaAlbaranCliente;

@Repository
public interface LineaAlbaranClienteRepository extends JpaRepository<LineaAlbaranCliente, Long> {
}

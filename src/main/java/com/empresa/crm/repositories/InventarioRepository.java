package com.empresa.crm.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.empresa.crm.entities.Inventario;

public interface InventarioRepository extends JpaRepository<Inventario, Long> {

	List<Inventario> findByEmpresaOrderByFechaDesc(String empresa);

}
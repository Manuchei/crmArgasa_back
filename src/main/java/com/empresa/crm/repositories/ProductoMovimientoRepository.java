package com.empresa.crm.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.empresa.crm.entities.ProductoMovimiento;

public interface ProductoMovimientoRepository extends JpaRepository<ProductoMovimiento, Long> {

	List<ProductoMovimiento> findByEmpresaOrderByFechaDesc(String empresa);

	List<ProductoMovimiento> findByEmpresaAndProductoIdOrderByFechaDesc(String empresa, Long productoId);
}
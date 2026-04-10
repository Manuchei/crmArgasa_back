package com.empresa.crm.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.empresa.crm.entities.AlbaranProveedor;

public interface AlbaranProveedorRepository extends JpaRepository<AlbaranProveedor, Long> {

	List<AlbaranProveedor> findByEmpresaOrderByFechaEmisionDescIdDesc(String empresa);

	List<AlbaranProveedor> findByProveedorIdOrderByFechaEmisionDescIdDesc(Long proveedorId);

	AlbaranProveedor findTopByEmpresaOrderByIdDesc(String empresa);
}
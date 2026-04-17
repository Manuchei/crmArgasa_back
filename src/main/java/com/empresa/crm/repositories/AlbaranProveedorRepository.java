package com.empresa.crm.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.empresa.crm.entities.AlbaranProveedor;

public interface AlbaranProveedorRepository extends JpaRepository<AlbaranProveedor, Long> {

	List<AlbaranProveedor> findByEmpresaOrderByFechaEmisionDescIdDesc(String empresa);

	List<AlbaranProveedor> findByProveedorIdOrderByFechaEmisionDescIdDesc(Long proveedorId);

	// ✅ NUEVO: filtrado correcto por empresa + proveedor
	List<AlbaranProveedor> findByProveedorIdAndEmpresaOrderByFechaEmisionDescIdDesc(Long proveedorId, String empresa);

	// ✅ NUEVO: evitar mezclar empresas en findById
	Optional<AlbaranProveedor> findByIdAndEmpresa(Long id, String empresa);

	AlbaranProveedor findTopByEmpresaOrderByIdDesc(String empresa);
}
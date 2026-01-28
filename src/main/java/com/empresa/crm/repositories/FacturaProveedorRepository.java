package com.empresa.crm.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


import com.empresa.crm.entities.FacturaProveedor;

@Repository
public interface FacturaProveedorRepository extends JpaRepository<FacturaProveedor, Long> {
	List<FacturaProveedor> findByEmpresa(String empresa);

	List<FacturaProveedor> findByPagada(boolean pagada);
	

	Optional<FacturaProveedor> findByIdAndEmpresa(Long id, String empresa);
	List<FacturaProveedor> findByProveedorIdAndEmpresa(Long proveedorId, String empresa);

}

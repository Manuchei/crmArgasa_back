package com.empresa.crm.services;

import java.util.List;

import com.empresa.crm.entities.FacturaProveedor;

public interface FacturaProveedorService {
	List<FacturaProveedor> findAll();

	FacturaProveedor findById(Long id);

	FacturaProveedor generarFactura(Long proveedorId, String empresa);

	FacturaProveedor marcarComoPagada(Long facturaId);

	List<FacturaProveedor> findByEmpresa(String empresa);

	List<FacturaProveedor> findByProveedor(Long proveedorId);
}
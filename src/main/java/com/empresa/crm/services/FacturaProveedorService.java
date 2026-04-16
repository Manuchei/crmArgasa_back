package com.empresa.crm.services;

import java.util.List;

import com.empresa.crm.entities.FacturaProveedor;

public interface FacturaProveedorService {

	List<FacturaProveedor> findAll();

	FacturaProveedor findById(Long id);

	FacturaProveedor generarFactura(Long proveedorId, String empresa);

	FacturaProveedor generarFacturaDesdeAlbaran(Long albaranId);

	FacturaProveedor guardarBorrador(Long facturaId, FacturaProveedor facturaEditada);

	FacturaProveedor emitirFactura(Long facturaId);

	FacturaProveedor marcarComoPagada(Long facturaId);

	FacturaProveedor actualizarNumeroFacturaProveedor(Long facturaId, String numeroFacturaProveedor);

	List<FacturaProveedor> findByEmpresa(String empresa);

	List<FacturaProveedor> findByProveedor(Long proveedorId);

	void eliminarBorrador(Long facturaId);
}
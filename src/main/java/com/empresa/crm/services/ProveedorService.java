package com.empresa.crm.services;

import java.util.List;

import com.empresa.crm.entities.Proveedor;

public interface ProveedorService {
	List<Proveedor> findAll();

	Proveedor findById(Long id);

	Proveedor save(Proveedor proveedor);

	void deleteById(Long id);

	List<Proveedor> findByOficio(String oficio);

	List<Proveedor> findByEmpresa(String empresa);
	
    // 🔍 Nuevo método
    List<Proveedor> buscar(String texto, String empresa, String oficio);
}


package com.empresa.crm.services;

import java.util.List;
import com.empresa.crm.entities.Trabajo;

public interface TrabajoService {
	List<Trabajo> findAll();

	Trabajo findById(Long id);

	Trabajo save(Trabajo trabajo);

	void deleteById(Long id);

	List<Trabajo> findByProveedor(Long proveedorId);

	List<Trabajo> findByPagado(boolean pagado);

	List<Trabajo> findByCliente(Long clienteId);
}

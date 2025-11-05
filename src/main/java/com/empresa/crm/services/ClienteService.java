package com.empresa.crm.services;

import java.util.List;

import com.empresa.crm.entities.Cliente;

public interface ClienteService {
	List<Cliente> findAll();

	Cliente findById(Long id);

	Cliente save(Cliente cliente);

	void deleteById(Long id);
}

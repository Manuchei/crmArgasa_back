package com.empresa.crm.services;

import java.time.LocalDate;
import java.util.List;

import com.empresa.crm.entities.FacturaCliente;

public interface FacturaClienteService {
	List<FacturaCliente> findAll();

	FacturaCliente findById(Long id);

	FacturaCliente generarFactura(Long clienteId, String empresa);

	FacturaCliente marcarComoPagada(Long facturaId);

	List<FacturaCliente> findByEmpresa(String empresa);

	List<FacturaCliente> findByCliente(Long clienteId);
	
	List<FacturaCliente> buscarInforme(String estado, Long clienteId, LocalDate desde, LocalDate hasta);
}
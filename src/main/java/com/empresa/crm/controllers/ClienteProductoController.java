package com.empresa.crm.controllers;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.empresa.crm.entities.ClienteProducto;
import com.empresa.crm.services.ClienteProductoService;

@RestController
@RequestMapping("/api/clientes")
@CrossOrigin
public class ClienteProductoController {

	private final  ClienteProductoService service;
	
	public ClienteProductoController(ClienteProductoService service) {
		this.service = service;

	}
	
	@PostMapping("/{clienteId}/productos/{productoId}")
	public ClienteProducto add (@PathVariable Long clienteId, @PathVariable Long productoId) {
		return service.addProductoToCliente(clienteId, productoId);
	}
	
}

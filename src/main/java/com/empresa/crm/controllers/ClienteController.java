package com.empresa.crm.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.empresa.crm.entities.Cliente;
import com.empresa.crm.repositories.ClienteRepository;
import com.empresa.crm.services.ClienteService;

@RestController
@RequestMapping("/api/clientes")
@CrossOrigin(origins = "http://localhost:4200") // frontend Angular
public class ClienteController {

	private final ClienteService clienteService;
	private final ClienteRepository clienteRepository; // ✅ Campo inyectado

	// ✅ Constructor único con inyección de dependencias
	public ClienteController(ClienteService clienteService, ClienteRepository clienteRepository) {
		this.clienteService = clienteService;
		this.clienteRepository = clienteRepository;
	}

	@GetMapping
	public List<Cliente> listarTodos() {
		return clienteService.findAll();
	}

	@GetMapping("/{id}")
	public Cliente obtenerPorId(@PathVariable Long id) {
		return clienteService.findById(id);
	}

	@PostMapping
	public Cliente crearCliente(@RequestBody Cliente cliente) {
		return clienteService.save(cliente);
	}

	@PutMapping("/{id}")
	public Cliente actualizarCliente(@PathVariable Long id, @RequestBody Cliente cliente) {
		cliente.setId(id);
		return clienteService.save(cliente);
	}

	@DeleteMapping("/{id}")
	public void eliminarCliente(@PathVariable Long id) {
		clienteService.deleteById(id);
	}

	@GetMapping("/buscar")
	public List<Cliente> buscarClientes(@RequestParam String texto, @RequestParam(required = false) String empresa) {

		if (empresa != null && !empresa.isBlank()) {
			return clienteRepository.buscarPorNombreYEmpresa(texto, empresa);
		} else {
			return clienteRepository.buscarPorNombreOApellido(texto);
		}
	}
}
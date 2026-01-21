package com.empresa.crm.controllers;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import com.empresa.crm.entities.Cliente;
import com.empresa.crm.entities.Trabajo;
import com.empresa.crm.repositories.ClienteRepository;
import com.empresa.crm.services.ClienteService;

@RestController
@RequestMapping("/api/clientes")
@CrossOrigin(origins = "http://localhost:4200")
public class ClienteController {

	private final ClienteService clienteService;
	private final ClienteRepository clienteRepository;

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

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public Cliente crearCliente(@RequestBody Cliente cliente) {

		if (cliente.getTrabajos() != null) {
			for (Trabajo t : cliente.getTrabajos()) {
				t.setCliente(cliente);
			}
		}

		double totalImporte = cliente.getTrabajos() != null
				? cliente.getTrabajos().stream().mapToDouble(t -> t.getImporte() != null ? t.getImporte() : 0).sum()
				: 0;

		double totalPagado = cliente.getTrabajos() != null ? cliente.getTrabajos().stream()
				.mapToDouble(t -> t.getImportePagado() != null ? t.getImportePagado() : 0).sum() : 0;

		cliente.setTotalImporte(totalImporte);
		cliente.setTotalPagado(totalPagado);

		return clienteService.save(cliente);
	}

	@PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
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
			return clienteRepository.buscarPorTextoYNombreComercial(texto, empresa);
		} else {
			return clienteRepository.buscarPorTexto(texto);
		}
	}

	@PostMapping(value = "/{id}/trabajos", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public Cliente agregarTrabajo(@PathVariable Long id, @RequestBody Trabajo trabajo) {
		Cliente cliente = clienteService.findById(id);

		if (cliente == null) {
			throw new RuntimeException("Cliente no encontrado");
		}

		trabajo.setCliente(cliente);
		cliente.getTrabajos().add(trabajo);

		double totalImporte = cliente.getTrabajos().stream()
				.mapToDouble(t -> t.getImporte() != null ? t.getImporte() : 0.0).sum();

		double totalPagado = cliente.getTrabajos().stream()
				.mapToDouble(t -> t.getImportePagado() != null ? t.getImportePagado() : 0.0).sum();

		cliente.setTotalImporte(totalImporte);
		cliente.setTotalPagado(totalPagado);

		return clienteService.save(cliente);
	}
}

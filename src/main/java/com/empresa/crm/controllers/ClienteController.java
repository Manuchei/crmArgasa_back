package com.empresa.crm.controllers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import com.empresa.crm.dto.ClienteResumenDTO;
import com.empresa.crm.entities.Cliente;
import com.empresa.crm.entities.Trabajo;
import com.empresa.crm.repositories.ClienteRepository;
import com.empresa.crm.services.ClienteService;
import com.empresa.crm.tenant.TenantContext;

import jakarta.validation.Valid;

@PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
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
	public List<ClienteResumenDTO> listarTodos() {
		String empresa = TenantContext.get();

		if (empresa == null || empresa.isBlank()) {
			throw new RuntimeException("Empresa no seleccionada (TenantContext vacío).");
		}

		return clienteRepository.findResumenByEmpresa(empresa);
	}

	@GetMapping("/{id}")
	public Cliente obtenerPorId(@PathVariable Long id) {
		return clienteService.findById(id);
	}

	@PostMapping
	public Cliente crearCliente(@Valid @RequestBody Cliente cliente) {

		String empresa = TenantContext.get();
		if (empresa == null || empresa.isBlank()) {
			throw new RuntimeException("Empresa no seleccionada (TenantContext vacío).");
		}
		cliente.setEmpresa(empresa);

		if (cliente.getNumeroCuenta() != null) {
			cliente.setNumeroCuenta(cliente.getNumeroCuenta().replaceAll("\\s+", "").toUpperCase().trim());
		}

		if (cliente.getTrabajos() != null) {
			for (Trabajo t : cliente.getTrabajos()) {
				t.setCliente(cliente);
				t.setEmpresa(empresa);
			}
		}

		double totalImporte = (cliente.getTrabajos() != null)
				? cliente.getTrabajos().stream().mapToDouble(t -> t.getImporte() != null ? t.getImporte() : 0.0).sum()
				: 0.0;

		double totalPagado = (cliente.getTrabajos() != null) ? cliente.getTrabajos().stream()
				.mapToDouble(t -> t.getImportePagado() != null ? t.getImportePagado() : 0.0).sum() : 0.0;

		cliente.setTotalImporte(totalImporte);
		cliente.setTotalPagado(totalPagado);

		return clienteService.save(cliente);
	}

	@PutMapping("/{id}")
	public Cliente actualizarCliente(@PathVariable Long id, @Valid @RequestBody Cliente cliente) {

		String empresa = TenantContext.get();
		if (empresa == null || empresa.isBlank()) {
			throw new RuntimeException("Empresa no seleccionada (TenantContext vacío).");
		}

		Cliente existente = clienteService.findById(id);
		if (existente == null) {
			throw new RuntimeException("Cliente no encontrado");
		}

		cliente.setId(id);
		cliente.setEmpresa(empresa);

		if (cliente.getNumeroCuenta() != null) {
			cliente.setNumeroCuenta(cliente.getNumeroCuenta().replaceAll("\\s+", "").toUpperCase().trim());
		}

		if (cliente.getTrabajos() != null) {
			for (Trabajo t : cliente.getTrabajos()) {
				t.setCliente(cliente);
				t.setEmpresa(empresa);
			}
		}

		double totalImporte = (cliente.getTrabajos() != null)
				? cliente.getTrabajos().stream().mapToDouble(t -> t.getImporte() != null ? t.getImporte() : 0.0).sum()
				: (existente.getTotalImporte() != null ? existente.getTotalImporte() : 0.0);

		double totalPagado = (cliente.getTrabajos() != null)
				? cliente.getTrabajos().stream()
						.mapToDouble(t -> t.getImportePagado() != null ? t.getImportePagado() : 0.0).sum()
				: (existente.getTotalPagado() != null ? existente.getTotalPagado() : 0.0);

		cliente.setTotalImporte(totalImporte);
		cliente.setTotalPagado(totalPagado);

		return clienteService.save(cliente);
	}

	@DeleteMapping("/{id}")
	public void eliminarCliente(@PathVariable Long id) {
		clienteService.deleteById(id);
	}

	@GetMapping("/buscar")
	public List<Cliente> buscarClientes(@RequestParam String texto) {

		String empresa = TenantContext.get();
		if (empresa == null || empresa.isBlank()) {
			throw new RuntimeException("Empresa no seleccionada (TenantContext vacío).");
		}

		return clienteRepository.buscarPorTexto(texto, empresa);
	}

	@PostMapping("/{id}/trabajos")
	public Cliente agregarTrabajo(@PathVariable Long id, @RequestBody Trabajo trabajo) {

		String empresa = TenantContext.get();
		if (empresa == null || empresa.isBlank()) {
			throw new RuntimeException("Empresa no seleccionada (TenantContext vacío).");
		}

		Cliente cliente = clienteService.findById(id);
		if (cliente == null) {
			throw new RuntimeException("Cliente no encontrado");
		}

		trabajo.setCliente(cliente);
		trabajo.setEmpresa(empresa);
		cliente.getTrabajos().add(trabajo);

		double totalImporte = cliente.getTrabajos().stream()
				.mapToDouble(t -> t.getImporte() != null ? t.getImporte() : 0.0).sum();

		double totalPagado = cliente.getTrabajos().stream()
				.mapToDouble(t -> t.getImportePagado() != null ? t.getImportePagado() : 0.0).sum();

		cliente.setTotalImporte(totalImporte);
		cliente.setTotalPagado(totalPagado);

		return clienteService.save(cliente);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, String>> handleValidationErrors(MethodArgumentNotValidException ex) {
		Map<String, String> errors = new LinkedHashMap<>();

		for (FieldError error : ex.getBindingResult().getFieldErrors()) {
			errors.put(error.getField(), error.getDefaultMessage());
		}

		return ResponseEntity.badRequest().body(errors);
	}
}
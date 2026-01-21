package com.empresa.crm.controllers;

import com.empresa.crm.dto.ResultadoGlobalDTO;
import com.empresa.crm.entities.Cliente;
import com.empresa.crm.entities.Proveedor;
import com.empresa.crm.repositories.ClienteRepository;
import com.empresa.crm.repositories.ProveedorRepository;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/buscar")
@CrossOrigin(origins = "http://localhost:4200")
public class BusquedaGlobalController {

	private final ClienteRepository clienteRepository;
	private final ProveedorRepository proveedorRepository;

	public BusquedaGlobalController(ClienteRepository clienteRepository, ProveedorRepository proveedorRepository) {
		this.clienteRepository = clienteRepository;
		this.proveedorRepository = proveedorRepository;
	}

	@GetMapping("/global")
	public List<ResultadoGlobalDTO> buscarGlobal(@RequestParam String texto,
			@RequestParam(required = false) String empresa) {

		List<ResultadoGlobalDTO> resultados = new ArrayList<>();

		// --- CLIENTES ---
		List<Cliente> clientes;
		if (empresa != null && !empresa.isBlank()) {
			clientes = clienteRepository.buscarPorTextoYNombreComercial(texto, empresa);
		} else {
			clientes = clienteRepository.buscarPorTexto(texto);
		}

		for (Cliente c : clientes) {
			resultados
					.add(new ResultadoGlobalDTO(c.getId(), c.getNombreApellidos(), c.getNombreComercial(), "Cliente"));
		}

		// --- PROVEEDORES ---
		List<Proveedor> proveedores;
		if (empresa != null && !empresa.isBlank()) {
			proveedores = proveedorRepository.buscarPorNombreYEmpresa(texto, empresa);
		} else {
			proveedores = proveedorRepository.buscarPorNombreOApellido(texto);
		}

		for (Proveedor p : proveedores) {
			String empresaProveedor = "";
			if (p.isTrabajaEnArgasa() && p.isTrabajaEnLuga())
				empresaProveedor = "Argasa y Luga";
			else if (p.isTrabajaEnArgasa())
				empresaProveedor = "Argasa";
			else if (p.isTrabajaEnLuga())
				empresaProveedor = "Luga";

			resultados.add(new ResultadoGlobalDTO(p.getId(), p.getNombre(), empresaProveedor, "Proveedor"));
		}

		return resultados;
	}
}

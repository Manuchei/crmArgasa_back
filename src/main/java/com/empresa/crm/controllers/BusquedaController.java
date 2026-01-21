package com.empresa.crm.controllers;

import com.empresa.crm.dto.ResultadoBusquedaDTO;
import com.empresa.crm.entities.Cliente;
import com.empresa.crm.entities.Proveedor;
import com.empresa.crm.repositories.ClienteRepository;
import com.empresa.crm.repositories.ProveedorRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/buscar")
@CrossOrigin(origins = "http://localhost:4200")
public class BusquedaController {

    private final ClienteRepository clienteRepository;
    private final ProveedorRepository proveedorRepository;

    public BusquedaController(ClienteRepository clienteRepository, ProveedorRepository proveedorRepository) {
        this.clienteRepository = clienteRepository;
        this.proveedorRepository = proveedorRepository;
    }

    @GetMapping
    public ResultadoBusquedaDTO buscarGlobal(@RequestParam String texto,
                                            @RequestParam(required = false) String empresa) {

        List<Cliente> clientes;
        List<Proveedor> proveedores;

        if (empresa != null && !empresa.isBlank()) {
            clientes = clienteRepository.buscarPorTextoYNombreComercial(texto, empresa);
            proveedores = proveedorRepository.buscarPorNombreYEmpresa(texto, empresa);
        } else {
            clientes = clienteRepository.buscarPorTexto(texto);
            proveedores = proveedorRepository.buscarPorNombreOApellido(texto);
        }

        return new ResultadoBusquedaDTO(clientes, proveedores);
    }
}

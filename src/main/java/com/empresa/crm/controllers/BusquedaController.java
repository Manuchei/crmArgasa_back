package com.empresa.crm.controllers;

import com.empresa.crm.dto.ResultadoBusquedaDTO;
import com.empresa.crm.entities.Cliente;
import com.empresa.crm.entities.Proveedor;
import com.empresa.crm.repositories.ClienteRepository;
import com.empresa.crm.repositories.ProveedorRepository;
import com.empresa.crm.tenant.TenantContext;

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
    public ResultadoBusquedaDTO buscarGlobal(@RequestParam String texto) {

        String empresa = TenantContext.get();

        List<Cliente> clientes = clienteRepository.buscarPorTexto(texto, empresa);
        List<Proveedor> proveedores = proveedorRepository.buscarPorNombreYEmpresa(texto, empresa);

        return new ResultadoBusquedaDTO(clientes, proveedores);
    }

}

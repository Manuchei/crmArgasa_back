package com.empresa.crm.controllers;

import com.empresa.crm.dto.ResultadoGlobalDTO;
import com.empresa.crm.entities.Cliente;
import com.empresa.crm.entities.Proveedor;
import com.empresa.crm.repositories.ClienteRepository;
import com.empresa.crm.repositories.ProveedorRepository;
import com.empresa.crm.tenant.TenantContext;

/*import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/buscar")
@CrossOrigin(origins = "http://localhost:4200")
public class BusquedaGlobalController {

    private final ClienteRepository clienteRepository;
    private final ProveedorRepository proveedorRepository;

    public BusquedaGlobalController(ClienteRepository clienteRepository,
                                    ProveedorRepository proveedorRepository) {
        this.clienteRepository = clienteRepository;
        this.proveedorRepository = proveedorRepository;
    }

    @GetMapping("/global")
    public List<ResultadoGlobalDTO> buscarGlobal(@RequestParam String texto) {

        String empresa = TenantContext.get();
        List<ResultadoGlobalDTO> resultados = new ArrayList<>();

        // ========= CLIENTES =========
        List<Cliente> clientes = clienteRepository.buscarPorTexto(texto, empresa);

        for (Cliente c : clientes) {
            resultados.add(new ResultadoGlobalDTO(
                    c.getId(),
                    c.getNombreApellidos(),
                    c.getNombreComercial(),
                    "cliente"
            ));
        }

        // ========= PROVEEDORES =========
        List<Proveedor> proveedores = proveedorRepository.buscarPorNombreYEmpresa(texto, empresa);

        for (Proveedor p : proveedores) {
            resultados.add(new ResultadoGlobalDTO(
                    p.getId(),
                    p.getNombre() + " " + p.getApellido(),
                    p.getOficio(),
                    "proveedor"
            ));
        }

        return resultados;
    }
}
*/
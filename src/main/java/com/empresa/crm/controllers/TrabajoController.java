package com.empresa.crm.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.empresa.crm.entities.Cliente;
import com.empresa.crm.entities.Proveedor;
import com.empresa.crm.entities.Trabajo;
import com.empresa.crm.services.ClienteService;
import com.empresa.crm.services.ProveedorService;
import com.empresa.crm.services.TrabajoService;

@RestController
@RequestMapping("/api/trabajos")
@CrossOrigin(origins = "http://localhost:4200")
public class TrabajoController {

    private final TrabajoService trabajoService;
    private final ClienteService clienteService;
    private final ProveedorService proveedorService;

    public TrabajoController(
            TrabajoService trabajoService,
            ClienteService clienteService,
            ProveedorService proveedorService) {

        this.trabajoService = trabajoService;
        this.clienteService = clienteService;
        this.proveedorService = proveedorService;
    }

    // -------------------- CRUD GENERAL --------------------

    @GetMapping
    public List<Trabajo> listarTodos() {
        return trabajoService.findAll();
    }

    @GetMapping("/{id}")
    public Trabajo obtenerPorId(@PathVariable Long id) {
        return trabajoService.findById(id);
    }

    @PostMapping
    public Trabajo crear(@RequestBody Trabajo trabajo) {
        return trabajoService.save(trabajo);
    }

    @PutMapping("/{id}")
    public Trabajo actualizar(@PathVariable Long id, @RequestBody Trabajo trabajo) {
        trabajo.setId(id);
        return trabajoService.save(trabajo);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        trabajoService.deleteById(id);
    }

    // -------------------- FILTROS --------------------

    @GetMapping("/proveedor/{proveedorId}")
    public List<Trabajo> listarPorProveedor(@PathVariable Long proveedorId) {
        return trabajoService.findByProveedor(proveedorId);
    }

    @GetMapping("/pagado/{pagado}")
    public List<Trabajo> listarPorPago(@PathVariable boolean pagado) {
        return trabajoService.findByPagado(pagado);
    }

    @GetMapping("/cliente/{clienteId}")
    public List<Trabajo> listarPorCliente(@PathVariable Long clienteId) {
        return trabajoService.findByCliente(clienteId);
    }

    // -------------------- CLIENTES --------------------

    @PostMapping("/cliente/{clienteId}")
    public Trabajo crearTrabajoParaCliente(@PathVariable Long clienteId, @RequestBody Trabajo trabajo) {

        Cliente cliente = clienteService.findById(clienteId);

        if (cliente == null) {
            throw new RuntimeException("Cliente no encontrado con ID: " + clienteId);
        }

        cliente.addTrabajo(trabajo);
        clienteService.save(cliente);

        return trabajo;
    }

    // -------------------- PROVEEDORES --------------------

    @PostMapping("/proveedor/{proveedorId}")
    public Trabajo crearTrabajoParaProveedor(@PathVariable Long proveedorId,
                                              @RequestBody Trabajo trabajo) {

        Proveedor proveedor = proveedorService.findById(proveedorId);

        if (proveedor == null) {
            throw new RuntimeException("Proveedor no encontrado con ID: " + proveedorId);
        }

        trabajo.setProveedor(proveedor);
        trabajoService.save(trabajo);

        proveedor.getTrabajos().add(trabajo);

        // ✅ Recalcular totales del proveedor
        proveedorService.save(proveedor);

        return trabajo;
    }

    @DeleteMapping("/proveedor/{trabajoId}")
    public void eliminarTrabajoProveedor(@PathVariable Long trabajoId) {

        Trabajo trabajo = trabajoService.findById(trabajoId);

        if (trabajo == null) {
            throw new RuntimeException("Trabajo no encontrado con ID: " + trabajoId);
        }

        Proveedor proveedor = trabajo.getProveedor();

        trabajoService.deleteById(trabajoId);

        proveedor.getTrabajos().remove(trabajo);

        // ✅ Recalcular totales
        proveedorService.save(proveedor);
    }
}

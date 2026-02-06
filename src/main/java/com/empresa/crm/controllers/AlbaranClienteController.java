package com.empresa.crm.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.empresa.crm.entities.AlbaranCliente;
import com.empresa.crm.entities.LineaAlbaranCliente;
import com.empresa.crm.services.AlbaranClienteService;

@RestController
@RequestMapping("/api/albaranes")
@CrossOrigin(origins = "http://localhost:4200")
public class AlbaranClienteController {

    private final AlbaranClienteService service;

    public AlbaranClienteController(AlbaranClienteService service) {
        this.service = service;
    }

    @GetMapping
    public List<AlbaranCliente> listar(@RequestParam(required = false) String empresa,
                                      @RequestParam(required = false) Long clienteId) {

        if (clienteId != null) return service.findByCliente(clienteId);
        if (empresa != null && !empresa.isBlank()) return service.findByEmpresa(empresa);
        return service.findAll();
    }

    @GetMapping("/clientes/{clienteId}")
    public List<AlbaranCliente> listarPorCliente(@PathVariable Long clienteId) {
        return service.findByCliente(clienteId);
    }

    @GetMapping("/{id}")
    public AlbaranCliente detalle(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping("/clientes/{clienteId}")
    public AlbaranCliente crearDesdeClientePath(@PathVariable Long clienteId) {
        return service.crearDesdeCliente(clienteId);
    }

    @PutMapping("/{id}")
    public AlbaranCliente actualizar(@PathVariable Long id, @RequestBody AlbaranCliente albaran) {
        albaran.setId(id);
        return service.save(albaran);
    }

    @DeleteMapping("/{id}")
    public void borrar(@PathVariable Long id) {
        service.deleteById(id);
    }

    @PostMapping("/{id}/lineas")
    public AlbaranCliente addLinea(@PathVariable Long id, @RequestBody LineaAlbaranCliente linea) {
        return service.agregarLinea(id, linea);
    }

    @DeleteMapping("/{id}/lineas/{lineaId}")
    public AlbaranCliente deleteLinea(@PathVariable Long id, @PathVariable Long lineaId) {
        return service.eliminarLinea(id, lineaId);
    }

    @PostMapping("/{id}/confirmar")
    public AlbaranCliente confirmar(@PathVariable Long id) {
        return service.confirmar(id);
    }
}

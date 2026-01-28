package com.empresa.crm.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.empresa.crm.entities.FacturaCliente;
import com.empresa.crm.services.FacturaClienteService;
import com.empresa.crm.tenant.TenantContext;

@RestController
@RequestMapping("/api/facturas-clientes")
@CrossOrigin(origins = "http://localhost:4200")
public class FacturaClienteController {

    private final FacturaClienteService facturaService;

    public FacturaClienteController(FacturaClienteService facturaService) {
        this.facturaService = facturaService;
    }

    @GetMapping
    public List<FacturaCliente> listarTodas() {
        // ✅ Ideal: el service ya filtra por empresa usando TenantContext
        return facturaService.findAll();
    }

    @GetMapping("/cliente/{clienteId}")
    public List<FacturaCliente> listarPorCliente(@PathVariable Long clienteId) {
        // ✅ Ideal: el service valida que el cliente pertenece a la empresa del TenantContext
        return facturaService.findByCliente(clienteId);
    }

    // ✅ Ahora NO pedimos empresa por URL: la sacamos del TenantContext
    @PostMapping("/generar/{clienteId}")
    public ResponseEntity<?> generar(@PathVariable Long clienteId) {

        String empresa = TenantContext.get();
        if (empresa == null || empresa.isBlank()) {
            return ResponseEntity.badRequest().body("Empresa no seleccionada (TenantContext vacío).");
        }

        FacturaCliente factura = facturaService.generarFactura(clienteId, empresa);

        if (factura == null) {
            return ResponseEntity.badRequest().body("DEBUG: cliente no existe en empresa o no hay servicios sin factura.");
        }

        return ResponseEntity.ok(factura);
    }


    @PutMapping("/pagar/{facturaId}")
    public FacturaCliente pagar(@PathVariable Long facturaId) {
        // ✅ Ideal: el service valida empresa por TenantContext
        return facturaService.marcarComoPagada(facturaId);
    }

    // ❌ Eliminado: ya no tiene sentido pasar empresa por URL
    // @GetMapping("/empresa/{empresa}")
    // public List<FacturaCliente> listarPorEmpresa(@PathVariable String empresa) { ... }
}

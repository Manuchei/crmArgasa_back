package com.empresa.crm.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.empresa.crm.entities.Proveedor;
import com.empresa.crm.entities.Trabajo;
import com.empresa.crm.services.ProveedorService;
import com.empresa.crm.services.TrabajoService;

@RestController
@RequestMapping("/api/trabajos-proveedor")
@CrossOrigin(origins = "http://localhost:4200")
public class TrabajoProveedorController {

    private final TrabajoService trabajoService;
    private final ProveedorService proveedorService;

    public TrabajoProveedorController(
            TrabajoService trabajoService,
            ProveedorService proveedorService) {
        this.trabajoService = trabajoService;
        this.proveedorService = proveedorService;
    }

    // ✅ Obtener trabajos por proveedor
    @GetMapping("/proveedor/{proveedorId}")
    public List<Trabajo> getTrabajosPorProveedor(@PathVariable Long proveedorId) {
        return trabajoService.findByProveedor(proveedorId);
    }

    // ✅ Crear trabajo para proveedor
    @PostMapping("/proveedor/{proveedorId}")
    public Trabajo crearTrabajoProveedor(
            @PathVariable Long proveedorId,
            @RequestBody Trabajo trabajo) {

        Proveedor proveedor = proveedorService.findById(proveedorId);

        if (proveedor == null) {
            throw new RuntimeException("Proveedor no encontrado con ID: " + proveedorId);
        }

        trabajo.setProveedor(proveedor);

        // Guardar trabajo
        Trabajo guardado = trabajoService.save(trabajo);

        // Añadir a la lista del proveedor
        proveedor.getTrabajos().add(guardado);

        // ✅ Recalcular totales del proveedor
        proveedorService.save(proveedor);

        return guardado;
    }

    // ✅ Eliminar trabajo de un proveedor
    @DeleteMapping("/{trabajoId}")
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

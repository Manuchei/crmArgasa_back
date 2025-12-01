package com.empresa.crm.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import com.empresa.crm.entities.Proveedor;
import com.empresa.crm.repositories.ProveedorRepository;
import com.empresa.crm.services.ProveedorService;

@RestController
@RequestMapping("/api/proveedores")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class ProveedorController {

    private final ProveedorService proveedorService;

    @GetMapping
    public List<Proveedor> listarTodos() {
        return proveedorService.findAll();
    }

    @GetMapping("/{id}")
    public Proveedor obtenerPorId(@PathVariable Long id) {
        return proveedorService.findById(id);
    }

    @PostMapping
    public Proveedor crear(@RequestBody Proveedor proveedor) {

        if (proveedor.isTrabajaEnArgasa()) proveedor.setEmpresa("argasa");
        if (proveedor.isTrabajaEnLuga()) proveedor.setEmpresa("luga");

        return proveedorService.save(proveedor);
    }

    @PutMapping("/{id}")
    public Proveedor actualizar(@PathVariable Long id, @RequestBody Proveedor proveedor) {
        proveedor.setId(id);
        return proveedorService.save(proveedor);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        proveedorService.deleteById(id);
    }

    // ⭐ FILTRO COMBINADO
    @GetMapping("/buscar")
    public List<Proveedor> buscar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) String empresa,
            @RequestParam(required = false) String oficio) {

        return proveedorService.buscar(
                texto == null ? "" : texto,
                empresa == null ? "" : empresa,
                oficio == null ? "" : oficio
        );
    }

}

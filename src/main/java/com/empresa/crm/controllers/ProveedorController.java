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
    private final ProveedorRepository proveedorRepository;
    
    // LISTAR TODOS
    @GetMapping
    public List<Proveedor> listarTodos() {
        return proveedorService.findAll();
    }

    // OBTENER POR ID
    @GetMapping("/{id}")
    public Proveedor obtenerPorId(@PathVariable Long id) {
        return proveedorService.findById(id);
    }

    // CREAR PROVEEDOR (POST)
    @PostMapping
    public Proveedor crear(@RequestBody Proveedor proveedor) {

        // Asignar empresa automáticamente
        if (proveedor.isTrabajaEnArgasa()) {
            proveedor.setEmpresa("argasa");
        } else if (proveedor.isTrabajaEnLuga()) {
            proveedor.setEmpresa("luga");
        }

        return proveedorService.save(proveedor);
    }

    // ACTUALIZAR PROVEEDOR
    @PutMapping("/{id}")
    public Proveedor actualizar(@PathVariable Long id, @RequestBody Proveedor proveedor) {
        proveedor.setId(id);
        return proveedorService.save(proveedor);
    }

    // ELIMINAR PROVEEDOR
    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        proveedorService.deleteById(id);
    }

    // LISTAR POR OFICIO
    @GetMapping("/oficio/{oficio}")
    public List<Proveedor> listarPorOficio(@PathVariable String oficio) {
        return proveedorService.findByOficio(oficio);
    }

    // LISTAR POR EMPRESA
    @GetMapping("/empresa/{empresa}")
    public List<Proveedor> listarPorEmpresa(@PathVariable String empresa) {
        return proveedorService.findByEmpresa(empresa);
    }

    // BUSCAR PROVEEDORES (sin empresa o con empresa)
    @GetMapping("/buscar")
    public List<Proveedor> buscarProveedores(
            @RequestParam String texto,
            @RequestParam(required = false) String empresa) {

        if (empresa != null && !empresa.isBlank()) {
            return proveedorRepository.buscarPorNombreYEmpresa(texto, empresa);
        }

        return proveedorRepository.buscarPorNombreOApellido(texto);
    }

    // LISTAR SOLO ARGASA
    @GetMapping("/argasa")
    public List<Proveedor> getArgasa() {
        return proveedorRepository.findByTrabajaEnArgasaTrue();
    }

    // LISTAR SOLO LUGA
    @GetMapping("/luga")
    public List<Proveedor> getLuga() {
        return proveedorRepository.findByTrabajaEnLugaTrue();
    }
}

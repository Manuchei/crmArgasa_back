package com.empresa.crm.controllers;

import java.time.LocalDate;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.empresa.crm.dto.RutaDiaRequestDTO;
import com.empresa.crm.entities.Ruta;
import com.empresa.crm.services.RutaService;

@RestController
@RequestMapping("/api/rutas")
@CrossOrigin(origins = "http://localhost:4200")
public class RutaController {

    private final RutaService rutaService;

    public RutaController(RutaService rutaService) {
        this.rutaService = rutaService;
    }

    @GetMapping
    public List<Ruta> listarTodas() {
        return rutaService.findAll();
    }

    @GetMapping("/{id}")
    public Ruta obtenerPorId(@PathVariable Long id) {
        return rutaService.findById(id);
    }

    // ✅ Acepta empresa en BODY o en HEADER X-Empresa
    @PostMapping
    public Ruta crear(@RequestBody Ruta ruta,
                      @RequestHeader(value = "X-Empresa", required = false) String empresaHeader) {

        if ((ruta.getEmpresa() == null || ruta.getEmpresa().isBlank())
                && empresaHeader != null && !empresaHeader.isBlank()) {
            ruta.setEmpresa(empresaHeader.trim());
        }

        if (ruta.getEmpresa() == null || ruta.getEmpresa().isBlank()) {
            throw new IllegalArgumentException("Empresa obligatoria (ARGASA / ELECTROLUGA). Envíala en el body (empresa) o en header X-Empresa.");
        }

        return rutaService.save(ruta);
    }

    @PutMapping("/{id}")
    public Ruta actualizar(@PathVariable Long id,
                           @RequestBody Ruta ruta,
                           @RequestHeader(value = "X-Empresa", required = false) String empresaHeader) {

        ruta.setId(id);

        if ((ruta.getEmpresa() == null || ruta.getEmpresa().isBlank())
                && empresaHeader != null && !empresaHeader.isBlank()) {
            ruta.setEmpresa(empresaHeader.trim());
        }

        if (ruta.getEmpresa() == null || ruta.getEmpresa().isBlank()) {
            throw new IllegalArgumentException("Empresa obligatoria (ARGASA / ELECTROLUGA). Envíala en el body (empresa) o en header X-Empresa.");
        }

        return rutaService.save(ruta);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        rutaService.deleteById(id);
    }

    @GetMapping("/estado/{estado}")
    public List<Ruta> filtrarPorEstado(@PathVariable String estado) {
        return rutaService.findByEstado(estado);
    }

    @GetMapping("/transportista/{nombre}")
    public List<Ruta> filtrarPorTransportista(@PathVariable String nombre) {
        return rutaService.findByNombreTransportista(nombre);
    }

    @GetMapping("/fecha/{fecha}")
    public List<Ruta> filtrarPorFecha(@PathVariable String fecha) {
        LocalDate f = LocalDate.parse(fecha);
        return rutaService.findByFecha(f);
    }

    @PutMapping("/cerrar/{id}")
    public Ruta cerrarRuta(@PathVariable Long id) {
        return rutaService.cerrarRuta(id);
    }

    @PostMapping("/dia")
    public List<Ruta> crearRutasDia(@RequestBody RutaDiaRequestDTO request,
                                    @RequestHeader(value = "X-Empresa", required = false) String empresaHeader) {

        if ((request.getEmpresa() == null || request.getEmpresa().isBlank())
                && empresaHeader != null && !empresaHeader.isBlank()) {
            request.setEmpresa(empresaHeader.trim());
        }

        if (request.getEmpresa() == null || request.getEmpresa().isBlank()) {
            throw new IllegalArgumentException("Empresa obligatoria (ARGASA / ELECTROLUGA)");
        }

        return rutaService.crearRutasDeUnDia(request);
    }

}

package com.empresa.crm.controllers;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.empresa.crm.dto.RutaDiaRequestDTO;
import com.empresa.crm.dto.RutaLineaDTO;
import com.empresa.crm.dto.RutaRequestDTO;
import com.empresa.crm.entities.Cliente;
import com.empresa.crm.entities.Producto;
import com.empresa.crm.entities.Ruta;
import com.empresa.crm.entities.RutaLinea;
import com.empresa.crm.entities.Transportista;
import com.empresa.crm.repositories.ClienteRepository;
import com.empresa.crm.repositories.ProductoRepository;
import com.empresa.crm.repositories.TransportistaRepository;
import com.empresa.crm.services.RutaService;

@PreAuthorize("hasRole('ADMIN') or hasRole('TRANSPORTISTA')")
@RestController
@RequestMapping("/api/rutas")
@CrossOrigin(origins = "http://localhost:4200")
public class RutaController {

    private final RutaService rutaService;
    private final ClienteRepository clienteRepo;
    private final ProductoRepository productoRepo;
    private final TransportistaRepository transportistaRepo;

    public RutaController(
            RutaService rutaService,
            ClienteRepository clienteRepo,
            ProductoRepository productoRepo,
            TransportistaRepository transportistaRepo
    ) {
        this.rutaService = rutaService;
        this.clienteRepo = clienteRepo;
        this.productoRepo = productoRepo;
        this.transportistaRepo = transportistaRepo;
    }

    @GetMapping
    public List<Ruta> listarTodas() {
        return rutaService.findAll();
    }

    @GetMapping("/{id}")
    public Ruta obtenerPorId(@PathVariable Long id) {
        return rutaService.findById(id);
    }

    @PostMapping
    public Ruta crear(
            @RequestBody RutaRequestDTO dto,
            @RequestHeader(value = "X-Empresa", required = false) String empresaHeader
    ) {

        if (dto.getClienteId() == null) {
            throw new IllegalArgumentException("Cliente obligatorio (clienteId).");
        }

        if (dto.getTransportistaId() == null) {
            throw new IllegalArgumentException("Transportista obligatorio (transportistaId).");
        }

        Cliente cliente = clienteRepo.findById(dto.getClienteId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        String empresa = (dto.getEmpresa() != null && !dto.getEmpresa().isBlank())
                ? dto.getEmpresa().trim()
                : (empresaHeader != null && !empresaHeader.isBlank())
                ? empresaHeader.trim()
                : (cliente.getEmpresa() != null ? cliente.getEmpresa().trim() : null);

        if (empresa == null || empresa.isBlank()) {
            throw new IllegalArgumentException("Empresa obligatoria (ARGASA / ELECTROLUGA).");
        }

        Transportista transportista = transportistaRepo.findById(dto.getTransportistaId())
                .orElseThrow(() -> new RuntimeException("Transportista no encontrado"));

        if (transportista.getEmpresa() != null
                && !transportista.getEmpresa().equalsIgnoreCase(empresa)) {
            throw new IllegalArgumentException(
                    "El transportista no pertenece a la empresa " + empresa
            );
        }

        Ruta ruta = new Ruta();
        ruta.setCliente(cliente);

        ruta.setTransportista(transportista);
        ruta.setNombreTransportista(transportista.getNombre());
        ruta.setEmailTransportista(transportista.getEmail());

        ruta.setFecha(dto.getFecha());
        ruta.setEstado(dto.getEstado());

        ruta.setOrigen(dto.getOrigen());
        ruta.setDestino(dto.getDestino());
        ruta.setTarea(dto.getTarea());
        ruta.setObservaciones(dto.getObservaciones());

        ruta.setEmpresa(empresa);

        if (dto.getLineas() != null && !dto.getLineas().isEmpty()) {

            List<RutaLinea> lineas = new ArrayList<>();

            for (RutaLineaDTO l : dto.getLineas()) {
                if (l.getProductoId() == null) {
                    throw new IllegalArgumentException("productoId obligatorio en lineas");
                }
                if (l.getCantidad() == null || l.getCantidad() <= 0) {
                    throw new IllegalArgumentException("cantidad > 0 obligatoria en lineas");
                }

                Producto p = productoRepo.findById(l.getProductoId())
                        .orElseThrow(() -> new RuntimeException(
                        "Producto no encontrado: " + l.getProductoId()
                ));

                if (p.getEmpresa() != null && !p.getEmpresa().equalsIgnoreCase(empresa)) {
                    throw new IllegalArgumentException(
                            "El producto " + p.getId() + " no pertenece a la empresa " + empresa
                    );
                }

                RutaLinea rl = new RutaLinea();
                rl.setRuta(ruta);
                rl.setProducto(p);
                rl.setCantidad(l.getCantidad());
                rl.setEstado("PENDIENTE");

                lineas.add(rl);
            }

            ruta.setLineas(lineas);
        }

        return rutaService.save(ruta);
    }

    @PutMapping("/{id}")
    public Ruta actualizar(
            @PathVariable Long id,
            @RequestBody RutaRequestDTO dto,
            @RequestHeader(value = "X-Empresa", required = false) String empresaHeader
    ) {

        if (dto.getClienteId() == null) {
            throw new IllegalArgumentException("Cliente obligatorio (clienteId).");
        }

        Cliente cliente = clienteRepo.findById(dto.getClienteId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        String empresa = (dto.getEmpresa() != null && !dto.getEmpresa().isBlank())
                ? dto.getEmpresa().trim()
                : (empresaHeader != null && !empresaHeader.isBlank())
                ? empresaHeader.trim()
                : (cliente.getEmpresa() != null ? cliente.getEmpresa().trim() : null);

        if (empresa == null || empresa.isBlank()) {
            throw new IllegalArgumentException("Empresa obligatoria (ARGASA / ELECTROLUGA).");
        }

        Ruta ruta = rutaService.findById(id);
        if (ruta == null) {
            throw new RuntimeException("Ruta no encontrada");
        }

        ruta.setCliente(cliente);

        // ✅ SOLO cambiar transportista si viene informado
        if (dto.getTransportistaId() != null) {
            Transportista transportista = transportistaRepo.findById(dto.getTransportistaId())
                    .orElseThrow(() -> new RuntimeException("Transportista no encontrado"));

            if (transportista.getEmpresa() != null
                    && !transportista.getEmpresa().equalsIgnoreCase(empresa)) {
                throw new IllegalArgumentException(
                        "El transportista no pertenece a la empresa " + empresa
                );
            }

            ruta.setTransportista(transportista);
            ruta.setNombreTransportista(transportista.getNombre());
            ruta.setEmailTransportista(transportista.getEmail());
        } else {
            // ✅ compatibilidad con edición antigua
            if (dto.getNombreTransportista() != null && !dto.getNombreTransportista().isBlank()) {
                ruta.setNombreTransportista(dto.getNombreTransportista());
            }
            if (dto.getEmailTransportista() != null && !dto.getEmailTransportista().isBlank()) {
                ruta.setEmailTransportista(dto.getEmailTransportista());
            }
        }

        ruta.setFecha(dto.getFecha());
        ruta.setEstado(dto.getEstado());

        ruta.setOrigen(dto.getOrigen());
        ruta.setDestino(dto.getDestino());
        ruta.setTarea(dto.getTarea());
        ruta.setObservaciones(dto.getObservaciones());

        ruta.setEmpresa(empresa);

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
    public List<Ruta> crearRutasDia(
            @RequestBody RutaDiaRequestDTO request,
            @RequestHeader(value = "X-Empresa", required = false) String empresaHeader
    ) {

        if ((request.getEmpresa() == null || request.getEmpresa().isBlank())
                && empresaHeader != null
                && !empresaHeader.isBlank()) {
            request.setEmpresa(empresaHeader.trim());
        }

        if (request.getEmpresa() == null || request.getEmpresa().isBlank()) {
            throw new IllegalArgumentException("Empresa obligatoria (ARGASA / ELECTROLUGA)");
        }

        if (request.getTransportistaId() == null) {
            throw new IllegalArgumentException("Transportista obligatorio (transportistaId)");
        }

        Transportista transportista = transportistaRepo.findById(request.getTransportistaId())
                .orElseThrow(() -> new RuntimeException("Transportista no encontrado"));

        if (transportista.getEmpresa() != null
                && !transportista.getEmpresa().equalsIgnoreCase(request.getEmpresa())) {
            throw new IllegalArgumentException(
                    "El transportista no pertenece a la empresa " + request.getEmpresa()
            );
        }

        request.setNombreTransportista(transportista.getNombre());
        request.setEmailTransportista(transportista.getEmail());

        return rutaService.crearRutasDeUnDia(request);
    }
}

package com.empresa.crm.controllers;

import java.time.LocalDate;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.empresa.crm.dto.RutaDiaRequestDTO;
import com.empresa.crm.dto.RutaRequestDTO;
import com.empresa.crm.entities.Cliente;
import com.empresa.crm.entities.Ruta;
import com.empresa.crm.repositories.ClienteRepository;
import com.empresa.crm.services.RutaService;

@RestController
@RequestMapping("/api/rutas")
@CrossOrigin(origins = "http://localhost:4200")
public class RutaController {

	private final RutaService rutaService;
	private final ClienteRepository clienteRepo;

	public RutaController(RutaService rutaService, ClienteRepository clienteRepo) {
		this.rutaService = rutaService;
		this.clienteRepo = clienteRepo;
	}

	@GetMapping
	public List<Ruta> listarTodas() {
		return rutaService.findAll();
	}

	@GetMapping("/{id}")
	public Ruta obtenerPorId(@PathVariable Long id) {
		return rutaService.findById(id);
	}

	// ✅ CREAR: ahora recibe DTO con clienteId
	@PostMapping
	public Ruta crear(@RequestBody RutaRequestDTO dto,
			@RequestHeader(value = "X-Empresa", required = false) String empresaHeader) {

		if (dto.getClienteId() == null) {
			throw new IllegalArgumentException("Cliente obligatorio (clienteId).");
		}

		Cliente cliente = clienteRepo.findById(dto.getClienteId())
				.orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

		String empresa = (dto.getEmpresa() != null && !dto.getEmpresa().isBlank()) ? dto.getEmpresa().trim()
				: (empresaHeader != null && !empresaHeader.isBlank()) ? empresaHeader.trim()
						: (cliente.getEmpresa() != null ? cliente.getEmpresa().trim() : null);

		if (empresa == null || empresa.isBlank()) {
			throw new IllegalArgumentException("Empresa obligatoria (ARGASA / ELECTROLUGA).");
		}

		Ruta ruta = new Ruta();
		ruta.setCliente(cliente);

		ruta.setNombreTransportista(dto.getNombreTransportista());
		ruta.setEmailTransportista(dto.getEmailTransportista());
		ruta.setFecha(dto.getFecha());
		ruta.setEstado(dto.getEstado());

		ruta.setOrigen(dto.getOrigen());
		ruta.setDestino(dto.getDestino());
		ruta.setTarea(dto.getTarea());
		ruta.setObservaciones(dto.getObservaciones());

		ruta.setEmpresa(empresa);

		return rutaService.save(ruta);
	}

	// ✅ ACTUALIZAR: también con DTO
	@PutMapping("/{id}")
	public Ruta actualizar(@PathVariable Long id, @RequestBody RutaRequestDTO dto,
			@RequestHeader(value = "X-Empresa", required = false) String empresaHeader) {

		if (dto.getClienteId() == null) {
			throw new IllegalArgumentException("Cliente obligatorio (clienteId).");
		}

		Cliente cliente = clienteRepo.findById(dto.getClienteId())
				.orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

		String empresa = (dto.getEmpresa() != null && !dto.getEmpresa().isBlank()) ? dto.getEmpresa().trim()
				: (empresaHeader != null && !empresaHeader.isBlank()) ? empresaHeader.trim()
						: (cliente.getEmpresa() != null ? cliente.getEmpresa().trim() : null);

		if (empresa == null || empresa.isBlank()) {
			throw new IllegalArgumentException("Empresa obligatoria (ARGASA / ELECTROLUGA).");
		}

		Ruta ruta = rutaService.findById(id);
		if (ruta == null)
			throw new RuntimeException("Ruta no encontrada");

		ruta.setCliente(cliente);

		ruta.setNombreTransportista(dto.getNombreTransportista());
		ruta.setEmailTransportista(dto.getEmailTransportista());
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

	// (tu endpoint /dia lo dejamos como está; luego lo adaptamos a cliente por
	// fila)
	@PostMapping("/dia")
	public List<Ruta> crearRutasDia(@RequestBody RutaDiaRequestDTO request,
			@RequestHeader(value = "X-Empresa", required = false) String empresaHeader) {

		if ((request.getEmpresa() == null || request.getEmpresa().isBlank()) && empresaHeader != null
				&& !empresaHeader.isBlank()) {
			request.setEmpresa(empresaHeader.trim());
		}

		if (request.getEmpresa() == null || request.getEmpresa().isBlank()) {
			throw new IllegalArgumentException("Empresa obligatoria (ARGASA / ELECTROLUGA)");
		}

		return rutaService.crearRutasDeUnDia(request);
	}
}

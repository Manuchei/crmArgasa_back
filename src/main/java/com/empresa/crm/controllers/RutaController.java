package com.empresa.crm.controllers;

import java.time.LocalDate;
import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

	@PostMapping
	public Ruta crear(@RequestBody Ruta ruta) {
		return rutaService.save(ruta);
	}

	@PutMapping("/{id}")
	public Ruta actualizar(@PathVariable Long id, @RequestBody Ruta ruta) {
		ruta.setId(id);
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
	public List<Ruta> crearRutasDia(@RequestBody RutaDiaRequestDTO request) {
	    return rutaService.crearRutasDeUnDia(request);
	}

}

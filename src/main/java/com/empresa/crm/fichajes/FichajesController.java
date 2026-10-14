package com.empresa.crm.fichajes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import static com.empresa.crm.fichajes.FichajesService.*;

@RestController
@RequestMapping("/api/fichajes")
@PreAuthorize("hasAnyRole('USER','ADMIN','DEVELOPER','TRANSPORTISTA')")
public class FichajesController {

	private final FichajesService service;

	public FichajesController(FichajesService service) {
		this.service = service;
	}

	public record Operacion(String clave) {
	}

	public record Alta(Long usuarioId) {
	}

	public record Activo(boolean activo) {
	}

	public record Correccion(Instant entrada, Instant salida, String motivo, Long version) {
	}

	@GetMapping("/mi-estado")
	public MiDTO estado(Authentication a) {
		return service.miEstado(a.getName());
	}

	@PostMapping("/entrada")
	public JornadaDTO entrada(Authentication a, @RequestBody Operacion op) {

		return service.entrada(a.getName(), op.clave());
	}

	@PostMapping("/salida")
	public JornadaDTO salida(Authentication a, @RequestBody Operacion op) {

		return service.salida(a.getName(), op.clave());
	}

	@GetMapping("/mis-registros")
	public PaginaDTO misRegistros(Authentication a, @RequestParam LocalDate desde, @RequestParam LocalDate hasta,
			@RequestParam(defaultValue = "0") int pagina) {

		return service.historialPropio(a.getName(), desde, hasta, pagina);
	}

	@GetMapping("/empleados")
	@PreAuthorize("hasAnyRole('ADMIN','DEVELOPER')")
	public List<EmpleadoDTO> empleados() {
		return service.empleados();
	}

	@GetMapping("/usuarios-disponibles")
	@PreAuthorize("hasAnyRole('ADMIN','DEVELOPER')")
	public List<UsuarioDTO> candidatos() {
		return service.candidatos();
	}

	@PostMapping("/empleados")
	@PreAuthorize("hasAnyRole('ADMIN','DEVELOPER')")
	public EmpleadoDTO alta(@RequestBody Alta req) {
		return service.alta(req.usuarioId());
	}

	@PatchMapping("/empleados/{id}")
	@PreAuthorize("hasAnyRole('ADMIN','DEVELOPER')")
	public EmpleadoDTO activar(@PathVariable Long id, @RequestBody Activo req) {

		return service.activar(id, req.activo());
	}

	@GetMapping("/registros")
	@PreAuthorize("hasAnyRole('ADMIN','DEVELOPER')")
	public PaginaDTO registros(@RequestParam(required = false) Long empleadoId, @RequestParam LocalDate desde,
			@RequestParam LocalDate hasta, @RequestParam(defaultValue = "0") int pagina) {

		return service.historial(empleadoId, desde, hasta, pagina);
	}

	@PutMapping("/registros/{id}")
	@PreAuthorize("hasAnyRole('ADMIN','DEVELOPER')")
	public JornadaDTO corregir(Authentication a, @PathVariable Long id, @RequestBody Correccion req) {

		return service.corregir(id, req.entrada(), req.salida(), req.motivo(), req.version(), a.getName());
	}

	@GetMapping("/registros/{id}/correcciones")
	@PreAuthorize("hasAnyRole('ADMIN','DEVELOPER')")
	public List<CorreccionDTO> correcciones(Authentication a, @PathVariable Long id) {

		return service.correcciones(id, a.getName(), true);
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<Map<String, String>> error(ResponseStatusException ex) {

		return ResponseEntity.status(ex.getStatusCode())
				.body(Map.of("message", ex.getReason() == null ? "Operación no permitida" : ex.getReason()));
	}
}
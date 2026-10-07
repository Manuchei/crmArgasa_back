package com.empresa.crm.controllers;

import com.empresa.crm.entities.RegistroActividad;
import com.empresa.crm.entities.Usuario;
import com.empresa.crm.repositories.RegistroActividadRepository;
import com.empresa.crm.repositories.UsuarioRepository;
import com.empresa.crm.services.RegistroActividadService;
import com.empresa.crm.tenant.TenantContext;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@RestController
@RequestMapping("/api/developer")
@PreAuthorize("hasRole('DEVELOPER')")
public class GestionUsuariosController {

	private final UsuarioRepository usuarios;
	private final RegistroActividadRepository registros;
	private final RegistroActividadService auditoria;
	private final PasswordEncoder passwordEncoder;

	public GestionUsuariosController(UsuarioRepository usuarios, RegistroActividadRepository registros,
			RegistroActividadService auditoria, PasswordEncoder passwordEncoder) {

		this.usuarios = usuarios;
		this.registros = registros;
		this.auditoria = auditoria;
		this.passwordEncoder = passwordEncoder;
	}

	public record CuentaResponse(Long id, String nombre, String email, String rol) {
	}

	public record CuentaRequest(String nombre, String email, String rol, String password) {
	}

	@GetMapping("/usuarios")
	public List<CuentaResponse> listar() {

		return usuarios.findAll(Sort.by("nombre")).stream().map(this::respuesta).toList();
	}

	@GetMapping("/actividad")
	public Page<RegistroActividad> actividad(@RequestParam(defaultValue = "0") int page) {

		Pageable pageable = PageRequest.of(Math.max(0, page), 50, Sort.by(Sort.Direction.DESC, "fecha", "id"));

		return registros.findAll(pageable);
	}

	@PostMapping("/usuarios")
	@ResponseStatus(HttpStatus.CREATED)
	@Transactional
	public CuentaResponse crear(@RequestBody CuentaRequest request, Principal principal) {

		Usuario usuario = new Usuario();

		aplicarDatos(usuario, request, true);

		usuarios.saveAndFlush(usuario);

		auditoria.registrar(principal.getName(), "USUARIO_CREADO", describir(usuario), 201, TenantContext.get());

		return respuesta(usuario);
	}

	@PutMapping("/usuarios/{id}")
	@Transactional
	public CuentaResponse modificar(@PathVariable Long id, @RequestBody CuentaRequest request, Principal principal) {

		Usuario usuario = buscar(id);

		String antes = describir(usuario);

		boolean esMiCuenta = usuario.getEmail().equalsIgnoreCase(principal.getName());

		String nuevoEmail = request.email() == null ? "" : request.email().trim();

		String nuevoRol = normalizarRol(request.rol());

		if (esMiCuenta && (!usuario.getEmail().equalsIgnoreCase(nuevoEmail) || !"DEVELOPER".equals(nuevoRol))) {

			throw error("No puedes cambiar tu propio email o rol desde este panel");
		}

		aplicarDatos(usuario, request, false);

		usuarios.saveAndFlush(usuario);

		boolean cambiaPassword = request.password() != null && !request.password().isBlank();

		String detalle = "Antes: " + antes + " | Después: " + describir(usuario)
				+ (cambiaPassword ? " | Contraseña cambiada" : "");

		auditoria.registrar(principal.getName(), "USUARIO_MODIFICADO", detalle, 200, TenantContext.get());

		return respuesta(usuario);
	}

	@DeleteMapping("/usuarios/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Transactional
	public void eliminar(@PathVariable Long id, Principal principal) {

		Usuario usuario = buscar(id);

		if (usuario.getEmail().equalsIgnoreCase(principal.getName())) {
			throw error("No puedes eliminar tu propia cuenta");
		}

		String detalle = describir(usuario);

		usuarios.delete(usuario);
		usuarios.flush();

		auditoria.registrar(principal.getName(), "USUARIO_ELIMINADO", detalle, 204, TenantContext.get());
	}

	private Usuario buscar(Long id) {

		return usuarios.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
	}

	private void aplicarDatos(Usuario usuario, CuentaRequest request, boolean nuevo) {

		String nombre = request.nombre() == null ? "" : request.nombre().trim();

		if (nombre.isBlank() || nombre.length() > 120) {
			throw error("Nombre obligatorio: máximo 120 caracteres");
		}

		String email = request.email() == null ? "" : request.email().trim().toLowerCase(Locale.ROOT);

		if (email.length() > 254 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {

			throw error("Email inválido");
		}

		usuarios.findByEmailIgnoreCase(email).ifPresent(existente -> {

			if (!Objects.equals(existente.getId(), usuario.getId())) {
				throw error("Ya existe una cuenta con ese email");
			}
		});

		String rol = normalizarRol(request.rol());

		if (!Set.of("USER", "ADMIN", "TRANSPORTISTA", "DEVELOPER").contains(rol)) {

			throw error("Rol inválido");
		}

		String password = request.password();

		if (nuevo && (password == null || password.isBlank())) {
			throw error("La contraseña es obligatoria");
		}

		if (password != null && !password.isBlank()) {

			if (password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72) {

				throw error("La contraseña debe tener al menos 12 caracteres " + "y no superar 72 bytes");
			}

			usuario.setPassword(passwordEncoder.encode(password));
		}

		usuario.setNombre(nombre);
		usuario.setEmail(email);
		usuario.setRol(rol);
	}

	private String normalizarRol(String rol) {

		if (rol == null) {
			return "";
		}

		String normalizado = rol.trim().toUpperCase(Locale.ROOT);

		return normalizado.startsWith("ROLE_") ? normalizado.substring(5) : normalizado;
	}

	private CuentaResponse respuesta(Usuario usuario) {

		return new CuentaResponse(usuario.getId(), usuario.getNombre(), usuario.getEmail(),
				normalizarRol(usuario.getRol()));
	}

	private String describir(Usuario usuario) {

		return "ID=" + usuario.getId() + ", nombre=" + usuario.getNombre() + ", email=" + usuario.getEmail() + ", rol="
				+ normalizarRol(usuario.getRol());
	}

	private ResponseStatusException error(String mensaje) {

		return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
	}
}
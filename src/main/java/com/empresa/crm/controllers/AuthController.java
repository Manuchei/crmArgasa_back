package com.empresa.crm.controllers;

import com.empresa.crm.dto.JwtResponse;
import com.empresa.crm.dto.LoginRequest;
import com.empresa.crm.entities.Usuario;
import com.empresa.crm.security.JwtUtil;
import com.empresa.crm.services.AuthService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.empresa.crm.dto.MeResponse;
import com.empresa.crm.tenant.TenantContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.GrantedAuthority;

import com.empresa.crm.repositories.UsuarioRepository;

import com.empresa.crm.services.RegistroActividadService;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;
	private final JwtUtil jwtUtil;
	private final AuthenticationManager authenticationManager;
	private final UsuarioRepository usuarioRepository;
	private final RegistroActividadService registroActividadService;

	public AuthController(AuthService authService, JwtUtil jwtUtil, AuthenticationManager authenticationManager,
			UsuarioRepository usuarioRepository, RegistroActividadService registroActividadService) {

		this.authService = authService;
		this.jwtUtil = jwtUtil;
		this.authenticationManager = authenticationManager;
		this.usuarioRepository = usuarioRepository;
		this.registroActividadService = registroActividadService;
	}

	@PostMapping("/login")
	public ResponseEntity<JwtResponse> login(@RequestBody LoginRequest req) {

		Authentication auth;

		try {
			auth = authenticationManager
					.authenticate(new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
		} catch (AuthenticationException ex) {

			registroActividadService.registrar("ANONIMO", "LOGIN_FALLIDO",
					"Intento de acceso con credenciales incorrectas", 401, null);

			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
		}

		UserDetails userDetails = (UserDetails) auth.getPrincipal();

		String rol = userDetails.getAuthorities().stream().findFirst().map(GrantedAuthority::getAuthority)
				.orElse("ROLE_USER");

		String token = jwtUtil.generarToken(userDetails.getUsername(), rol);

		registroActividadService.registrar(userDetails.getUsername(), "LOGIN_CORRECTO", "Inicio de sesión", 200, null);

		return ResponseEntity.ok(new JwtResponse(token, rol));
	}

	@GetMapping("/me")
	public ResponseEntity<MeResponse> me() {

		String email = SecurityContextHolder.getContext().getAuthentication().getName();

		String rol = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
				.map(GrantedAuthority::getAuthority).findFirst().orElse("ROLE_USER");

		String empresa = TenantContext.get();

		Usuario u = usuarioRepository.findByEmail(email)
				.orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

		return ResponseEntity.ok(new MeResponse(u.getId(), u.getNombre(), email, rol, empresa));
	}
}
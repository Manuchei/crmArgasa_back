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

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

	private final AuthService authService;
	private final JwtUtil jwtUtil;
	private final AuthenticationManager authenticationManager;
	private final UsuarioRepository usuarioRepository;

	public AuthController(AuthService authService, JwtUtil jwtUtil, AuthenticationManager authenticationManager,
			UsuarioRepository usuarioRepository) {
		this.authService = authService;
		this.jwtUtil = jwtUtil;
		this.authenticationManager = authenticationManager;
		this.usuarioRepository = usuarioRepository;
	}

	@PostMapping("/register")
	public Usuario register(@RequestBody Usuario usuario) {
		// AuthService ya encripta con BCrypt
		return authService.registrar(usuario);
	}

	@PostMapping("/login")
	public ResponseEntity<JwtResponse> login(@RequestBody LoginRequest req) {

		// Autenticación REAL: email + password contra UserDetailsService
		Authentication auth = authenticationManager
				.authenticate(new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));

		UserDetails userDetails = (UserDetails) auth.getPrincipal();

		// Sacamos el rol en formato "ROLE_X"
		String rol = userDetails.getAuthorities().stream().findFirst().map(a -> a.getAuthority()).orElse("ROLE_USER");

		// Generamos JWT
		String token = jwtUtil.generarToken(userDetails.getUsername(), rol);

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
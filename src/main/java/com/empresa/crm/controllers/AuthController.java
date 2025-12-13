package com.empresa.crm.controllers;

import com.empresa.crm.dto.JwtResponse;
import com.empresa.crm.dto.LoginRequest;
import com.empresa.crm.entities.Usuario;
import com.empresa.crm.security.JwtUtil;
import com.empresa.crm.services.AuthService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

	private final AuthService authService;
	private final JwtUtil jwtUtil;

	/*
	 * @Autowired private AuthenticationManager authenticationManager;
	 */

	public AuthController(AuthService authService, JwtUtil jwtUtil) {
		this.authService = authService;
		this.jwtUtil = jwtUtil;
	}

	@PostMapping("/register")
	public Usuario register(@RequestBody Usuario usuario) {
		return authService.registrar(usuario);
	}

	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody LoginRequest req) {

		System.out.println("🟡 LOGIN FAKE");
		System.out.println("EMAIL: " + req.getEmail());

		// Token falso (o uno real si quieres)
		String fakeToken = "FAKE_TOKEN_" + req.getEmail();

		return ResponseEntity.ok(new JwtResponse(fakeToken, "ROLE_USER"));
	}

	/*
	 * @PostMapping("/login") public ResponseEntity<?> login(@RequestBody
	 * LoginRequest req) {
	 * 
	 * System.out.println("📩 LOGIN RECIBIDO:"); System.out.println("EMAIL: " +
	 * req.getEmail()); System.out.println("PASSWORD RAW: " + req.getPassword());
	 * 
	 * try { Authentication auth = authenticationManager .authenticate(new
	 * UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
	 * 
	 * UserDetails userDetails = (UserDetails) auth.getPrincipal();
	 * 
	 * String email = userDetails.getUsername(); String rol =
	 * userDetails.getAuthorities().iterator().next().getAuthority();
	 * 
	 * // GENERAR TOKEN String token = jwtUtil.generarToken(email, rol);
	 * 
	 * // 🔥 DEVOLVER TOKEN + ROL return ResponseEntity.ok(new JwtResponse(token,
	 * rol));
	 * 
	 * } catch (Exception e) { return
	 * ResponseEntity.status(401).body("Usuario o contraseña incorrectos"); } }
	 */
}

package com.empresa.crm.controllers;

import com.empresa.crm.entities.Usuario;
import com.empresa.crm.security.JwtUtil;
import com.empresa.crm.services.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

	private final AuthService authService;
	private final JwtUtil jwtUtil;

	public AuthController(AuthService authService, JwtUtil jwtUtil) {
		this.authService = authService;
		this.jwtUtil = jwtUtil;
	}

	@PostMapping("/register")
	public Usuario register(@RequestBody Usuario usuario) {
		return authService.registrar(usuario);
	}

	@PostMapping("/login")
	public Map<String, String> login(@RequestBody Usuario usuario) {
	    Usuario user = authService.findByEmail(usuario.getEmail());
	    if (user != null) {
	        String token = jwtUtil.generarToken(user.getEmail(), user.getRol());
	        return Map.of("token", token, "rol", user.getRol());
	    } else {
	        throw new RuntimeException("Usuario o contraseña incorrectos");
	    }
	}


}

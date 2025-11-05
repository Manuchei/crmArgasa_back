package com.empresa.crm.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.crypto.SecretKey;

@Component
public class JwtUtil {

	private static final String SECRET_KEY = "claveSuperSecretaParaJwt12345678901234567890"; // al menos 32 chars
	private final SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

	// Generar token con email y rol
	public String generarToken(String email, String rol) {
		Map<String, Object> claims = new HashMap<>();
		claims.put("rol", rol);
		return createToken(claims, email);
	}

	private String createToken(Map<String, Object> claims, String subject) {
		return Jwts.builder().setClaims(claims).setSubject(subject).setIssuedAt(new Date(System.currentTimeMillis()))
				.setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) // 10 horas
				.signWith(key, SignatureAlgorithm.HS256).compact();
	}

	// ✅ Extraer nombre de usuario (email)
	public String extractUsername(String token) {
		return extractClaim(token, Claims::getSubject);
	}

	// ✅ Validar si el token es correcto y pertenece al usuario
	public boolean validateToken(String token, UserDetails userDetails) {
		final String username = extractUsername(token);
		return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
	}

	// --- Métodos auxiliares ---
	private boolean isTokenExpired(String token) {
		return extractExpiration(token).before(new Date());
	}

	private Date extractExpiration(String token) {
		return extractClaim(token, Claims::getExpiration);
	}

	public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
		final Claims claims = extractAllClaims(token);
		return claimsResolver.apply(claims);
	}

	private Claims extractAllClaims(String token) {
		return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
	}
}

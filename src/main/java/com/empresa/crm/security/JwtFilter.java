package com.empresa.crm.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

	private final JwtUtil jwtUtil;
	private final UsuarioDetailsService usuarioDetailsService;

	public JwtFilter(JwtUtil jwtUtil, @Lazy UsuarioDetailsService usuarioDetailsService) {
		this.jwtUtil = jwtUtil;
		this.usuarioDetailsService = usuarioDetailsService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String path = request.getRequestURI();

		// ✅ Solo excluir login y register (NO excluir /me)
		if (path.equals("/api/auth/login") || path.equals("/api/auth/register")) {
			filterChain.doFilter(request, response);
			return;
		}

		final String authHeader = request.getHeader("Authorization");
		String username = null;
		String jwt = null;

		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			jwt = authHeader.substring(7);
			try {
				username = jwtUtil.extractUsername(jwt);
			} catch (Exception e) {
				// Token malformado => seguimos sin autenticar
				filterChain.doFilter(request, response);
				return;
			}
		}

		if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
			UserDetails userDetails = usuarioDetailsService.loadUserByUsername(username);

			if (jwtUtil.validateToken(jwt, userDetails)) {
				UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails,
						null, userDetails.getAuthorities());
				authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authToken);
			}
		}

		filterChain.doFilter(request, response);
	}
}
package com.empresa.crm.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authorization.AuthorityAuthorizationManager;

import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	private final JwtFilter jwtFilter;

	public SecurityConfig(JwtFilter jwtFilter) {
		this.jwtFilter = jwtFilter;
	}

	@Bean
	public static RoleHierarchy roleHierarchy() {
		return RoleHierarchyImpl.fromHierarchy("ROLE_DEVELOPER > ROLE_ADMIN");
	}

	@Bean
	public static MethodSecurityExpressionHandler methodSecurityExpressionHandler(RoleHierarchy roleHierarchy) {

		DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();

		handler.setRoleHierarchy(roleHierarchy);

		return handler;
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http, RoleHierarchy roleHierarchy) throws Exception {

		AuthorityAuthorizationManager<RequestAuthorizationContext> permisosInventarios = AuthorityAuthorizationManager
				.hasAnyRole("ADMIN", "USER");

		permisosInventarios.setRoleHierarchy(roleHierarchy);

		http.csrf(csrf -> csrf.disable())

				.cors(cors -> cors.configurationSource(corsConfigurationSource()))

				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

				.authorizeHttpRequests(auth -> auth

						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

						.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()

						// El registro público está bloqueado.
						.requestMatchers("/api/auth/register").denyAll()

						// Solo DEVELOPER puede gestionar cuentas e historial.
						.requestMatchers("/api/developer/**").hasRole("DEVELOPER")

						.requestMatchers(HttpMethod.GET, "/api/push/public-key").permitAll()

						.requestMatchers(HttpMethod.POST, "/api/push/subscribe").permitAll()

						.requestMatchers(HttpMethod.POST, "/api/push/unsubscribe").permitAll()

						// ADMIN y USER; DEVELOPER hereda ADMIN.
						.requestMatchers("/api/inventarios/**").access(permisosInventarios)

						.requestMatchers(HttpMethod.POST, "/api/webhooks/facturadirecta").permitAll()

						.requestMatchers("/api/**").authenticated()

						.anyRequest().permitAll())

				.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {

		return request -> {

			CorsConfiguration configuration = new CorsConfiguration();

			configuration.setAllowedOriginPatterns(List.of("http://localhost:4200", "https://novexapp.es"));

			configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

			configuration.setAllowedHeaders(List.of("*"));
			configuration.setAllowCredentials(true);
			configuration.setExposedHeaders(List.of("Authorization"));

			return configuration;
		};
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {

		return configuration.getAuthenticationManager();
	}
}
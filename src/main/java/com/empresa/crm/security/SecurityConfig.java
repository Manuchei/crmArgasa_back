package com.empresa.crm.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.empresa.crm.services.AuthService;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

		http.csrf(csrf -> csrf.disable()).cors(cors -> cors.and())
				.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}

/*
 * private final JwtFilter jwtFilter;
 * 
 * public SecurityConfig(JwtFilter jwtFilter) { this.jwtFilter = jwtFilter; }
 * 
 * @Bean public CorsConfigurationSource corsConfigurationSource() {
 * CorsConfiguration config = new CorsConfiguration();
 * config.setAllowCredentials(true); // 🔥 VERY IMPORTANT
 * config.addAllowedOrigin("http://localhost:4200");
 * config.addAllowedHeader("*"); config.addAllowedMethod("*");
 * 
 * UrlBasedCorsConfigurationSource source = new
 * UrlBasedCorsConfigurationSource(); source.registerCorsConfiguration("/**",
 * config); return source; }
 * 
 * @Bean public SecurityFilterChain filterChain(HttpSecurity http) throws
 * Exception {
 * 
 * http.csrf(csrf -> csrf.disable()).cors(cors -> cors.and())
 * .authorizeHttpRequests( auth ->
 * auth.requestMatchers("/api/auth/**").permitAll().anyRequest().authenticated()
 * ) .sessionManagement(sess ->
 * sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) .formLogin(form
 * -> form.disable()).httpBasic(basic -> basic.disable());
 * 
 * // ❌ Ya no registramos userDetailsService manualmente //
 * http.userDetailsService(authService);
 * 
 * http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
 * 
 * return http.build(); }
 * 
 * @Bean public AuthenticationManager
 * authenticationManager(AuthenticationConfiguration config) throws Exception {
 * return config.getAuthenticationManager(); }
 * 
 * @Bean public PasswordEncoder passwordEncoder() { return new
 * BCryptPasswordEncoder(); } }
 */

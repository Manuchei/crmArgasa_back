package com.empresa.crm.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.empresa.crm.entities.Usuario;
import com.empresa.crm.repositories.UsuarioRepository;

@Service
public class UsuarioDetailsService implements UserDetailsService {

	private final UsuarioRepository usuarioRepository;

	public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
		this.usuarioRepository = usuarioRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		Usuario usuario = usuarioRepository.findByEmail(username)
				.orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

		String rol = usuario.getRol();

		if (rol == null || rol.isBlank()) {
			rol = "USER";
		}

		rol = rol.trim().toUpperCase();

		if (rol.startsWith("ROLE_")) {
			rol = rol.substring(5);
		}

		return User.builder().username(usuario.getEmail()).password(usuario.getPassword()).roles(rol).build();
	}
}
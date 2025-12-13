package com.empresa.crm.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.empresa.crm.entities.Usuario;
import com.empresa.crm.repositories.UsuarioRepository;

@Service
public class UsuarioDetailsService implements UserDetailsService {
	@Autowired
	private UsuarioRepository usuarioRepository;

	@Override
	public UserDetails loadUserByUsername(String username) {
		Usuario usuario = usuarioRepository.findByEmail(username)
				.orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

		return User.builder().username(usuario.getEmail()).password(usuario.getPassword()).roles(usuario.getRol())
				.build();
	}
}

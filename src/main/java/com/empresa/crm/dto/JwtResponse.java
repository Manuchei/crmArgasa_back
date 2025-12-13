package com.empresa.crm.dto;

public class JwtResponse {

	private String token;
	private String rol;

	public JwtResponse(String token, String rol) {
		this.token = token;
		this.rol = rol;
	}

	public String getToken() {
		return token;
	}

	public String getRol() {
		return rol;
	}
}

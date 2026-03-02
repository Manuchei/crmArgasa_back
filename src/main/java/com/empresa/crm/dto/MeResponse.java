package com.empresa.crm.dto;

public class MeResponse {
	private Long id;
	private String nombre;
	private String email;
	private String rol;
	private String empresa;

	public MeResponse() {
	}

	public MeResponse(Long id, String nombre, String email, String rol, String empresa) {
		this.id = id;
		this.nombre = nombre;
		this.email = email;
		this.rol = rol;
		this.empresa = empresa;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getRol() {
		return rol;
	}

	public void setRol(String rol) {
		this.rol = rol;
	}

	public String getEmpresa() {
		return empresa;
	}

	public void setEmpresa(String empresa) {
		this.empresa = empresa;
	}
}
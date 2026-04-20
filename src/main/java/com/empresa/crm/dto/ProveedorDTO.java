package com.empresa.crm.dto;

import java.time.LocalDate;

import lombok.Data;

@Data
public class ProveedorDTO {

	private Long id;

	private String nombre;
	private String oficio;
	private String empresa;
	private String telefono;
	private String email;

	private boolean trabajaEnArgasa;
	private boolean trabajaEnLuga;

	private String trabajoRealizado;

	private String direccion;
	private String cif;
	private LocalDate fechaAltaProveedor;
	private String localidad;
	private String codigoPostal;
	private String provincia;
	private String pais;
	private String contacto;
	private String datosBancarios;
	private String notas;
}
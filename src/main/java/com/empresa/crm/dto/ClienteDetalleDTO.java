package com.empresa.crm.dto;

import lombok.Data;

@Data
public class ClienteDetalleDTO {

	private Long id;
	private String empresa;
	private String nombreApellidos;

	private String direccion;
	private String codigoPostal;
	private String poblacion;
	private String provincia;

	private String direccionEntrega;
	private String codigoPostalEntrega;
	private String poblacionEntrega;
	private String provinciaEntrega;

	private String telefono;
	private String movil;
	private String cifDni;
	private String email;

	private Double totalImporte;
	private Double totalPagado;

	private String numeroCuenta;
}
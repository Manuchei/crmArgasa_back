package com.empresa.crm.dto;

import java.time.LocalDate;

import lombok.Data;

@Data
public class PagoClienteComprobanteDTO {

	private Long id;
	private String empresa;
	private LocalDate fecha;
	private Double importe;
	private String metodo;
	private String observaciones;

	private Long clienteId;
	private String clienteNombreApellidos;
	private String clienteCifDni;
	private String clienteDireccion;
	private String clienteCodigoPostal;
	private String clientePoblacion;
	private String clienteProvincia;
	private String clienteTelefono;
	private String clienteMovil;
	private String clienteEmail;

	// Datos empresa emisora
	private String empresaNombre;
	private String empresaCif;
	private String empresaDireccion;
	private String empresaCodigoPostal;
	private String empresaPoblacion;
	private String empresaProvincia;
	private String empresaTelefono;
	private String empresaEmail;
	private String empresaLogoUrl;
}
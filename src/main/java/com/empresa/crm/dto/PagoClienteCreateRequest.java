package com.empresa.crm.dto;

import java.time.LocalDate;

import lombok.Data;

@Data
public class PagoClienteCreateRequest {
	private LocalDate fecha;
	private Double importe;
	private String metodo;
	private String observaciones;
}

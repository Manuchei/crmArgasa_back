package com.empresa.crm.dto;

import lombok.Data;

@Data
public class PagoTrabajoRequest {
	private Double importe;
	private String metodo;
	private String observaciones;
}

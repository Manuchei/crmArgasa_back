package com.empresa.crm.dto;

import lombok.Data;

@Data
public class TareaRequestDTO {

	private String empresa;
	private String titulo;
	private String fecha;
	private String estado;
	private String observaciones;
}
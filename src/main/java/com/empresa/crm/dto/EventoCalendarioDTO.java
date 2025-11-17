package com.empresa.crm.dto;

import lombok.Data;

@Data
public class EventoCalendarioDTO {

	private Long id;
	private String title; // motivo
	private String fecha; // formato yyyy-MM-dd'T'HH:mm:ss
	private String estado;
	private String observaciones;
	private String start;

}

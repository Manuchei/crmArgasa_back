package com.empresa.crm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventoCalendarioDTO {
	private String title;
	private String start;
	private String end;
	private String estado;
}
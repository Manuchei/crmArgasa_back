package com.empresa.crm.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TContableLineaDTO {

	private LocalDate fecha;
	private String concepto;
	private Double importe;

}
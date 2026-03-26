package com.empresa.crm.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HistorialSaldoMovimientoDTO {

	private LocalDate fecha;
	private String tipo; // CARGO | ABONO
	private String concepto;
	private Double cargo;
	private Double abono;
	private Double saldoAcumulado;
}
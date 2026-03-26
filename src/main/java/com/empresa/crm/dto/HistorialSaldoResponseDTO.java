package com.empresa.crm.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HistorialSaldoResponseDTO {

	private Long clienteId;
	private String clienteNombre;
	private String empresa;
	private Double saldoFinal;
	private String estadoSaldo; // PENDIENTE | A_FAVOR | SALDADO
	private List<HistorialSaldoMovimientoDTO> movimientos;
}
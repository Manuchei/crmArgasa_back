package com.empresa.crm.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HistorialTContableResponseDTO {

	private Long clienteId;
	private String clienteNombre;
	private String empresa;

	private List<TContableLineaDTO> debe;
	private List<TContableLineaDTO> haber;

	private Double totalDebe;
	private Double totalHaber;
	private Double saldoFinal;
	private String estadoSaldo;

}
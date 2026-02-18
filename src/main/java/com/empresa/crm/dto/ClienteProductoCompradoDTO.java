package com.empresa.crm.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ClienteProductoCompradoDTO {
	private Long productoId;
	private String codigo;
	private String nombre;
	private Integer cantidad; // unidades compradas (sumadas)
	private boolean entregado;
	private LocalDate fechaEntrega;
}

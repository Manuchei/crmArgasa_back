package com.empresa.crm.dto;

import lombok.Data;

@Data
public class ClienteProductoAsignarDTO {
	private Long clienteId;
	private Long productoId;
	private Integer cantidadTotal; // opcional
}
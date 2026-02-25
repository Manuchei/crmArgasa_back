package com.empresa.crm.dto;

import lombok.Data;

@Data
public class ProductoCantidadDTO {
	private Long producto; // nombre/descripcion
	private Integer cantidad;
}
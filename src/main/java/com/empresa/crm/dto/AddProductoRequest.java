package com.empresa.crm.dto;

import lombok.Data;

@Data
public class AddProductoRequest {

	private Integer cantidad;
	private Double descuento;
	private Double importePagado;

}

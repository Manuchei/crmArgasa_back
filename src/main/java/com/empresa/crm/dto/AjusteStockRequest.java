package com.empresa.crm.dto;

import lombok.Data;

@Data
public class AjusteStockRequest {

	private Integer delta;
	private String motivo;
}
package com.empresa.crm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumenMensualSimpleDTO {
	private String mes; // formato "01", "02"...
	private double totalClientes;
	private double totalProveedores;
	private double beneficio;
}

package com.empresa.crm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultadoGlobalDTO {
	private Long id;
	private String nombre;
	private String empresa; // "Argasa" o "Luga"
	private String tipo; // "Cliente" o "Proveedor"
}

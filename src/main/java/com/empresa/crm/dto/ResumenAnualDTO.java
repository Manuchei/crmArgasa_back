package com.empresa.crm.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumenAnualDTO {
	private int año;
	private double beneficioTotal;
	private List<ResumenMensualSimpleDTO> meses;
}
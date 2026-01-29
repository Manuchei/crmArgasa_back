package com.empresa.crm.dto.facturacionv2;

import java.time.LocalDate;

public record ServicioPendienteDTO(
		
		Long id,
		String descripcion,
		LocalDate fecha,
		Double importe
		
		) {

}

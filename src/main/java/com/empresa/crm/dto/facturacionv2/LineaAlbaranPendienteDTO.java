package com.empresa.crm.dto.facturacionv2;

public record LineaAlbaranPendienteDTO(
		
		Long id,
		Long albaranId,
		String descripcion,
		Double unidades,
		Double precio,
		Double dtoPct,
		Double totalLinea
		
		) {

}

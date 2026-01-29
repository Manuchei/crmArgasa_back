package com.empresa.crm.dto.facturacionv2;

import java.time.LocalDate;
import java.util.List;

public record FacturaV2Response(
		
		Long id,
		String empresa,
		String serie,
		Integer numero,
		LocalDate fechaEmision,
		String estado,
		Double basaImponible,
		Double ivaTotal,
		Double total,
		List<LineaFacturaV2Response> lineas
		
		) {

}

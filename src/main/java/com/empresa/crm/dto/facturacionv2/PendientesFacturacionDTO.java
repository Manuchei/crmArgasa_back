package com.empresa.crm.dto.facturacionv2;

import java.util.List;

public record PendientesFacturacionDTO(
		
		List<ServicioPendienteDTO> servicios,
		List<LineaAlbaranPendienteDTO> lineasAlbaran
		
		) {

}

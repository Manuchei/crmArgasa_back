package com.empresa.crm.dto.facturacionv2;

import java.time.LocalDate;
import java.util.List;

public record FacturaV2Response(Long id, String empresa, String serie, Integer numero, LocalDate fechaEmision,
		String estado, Double baseImponible, Double ivaTotal, Double total, ClienteDTO cliente,
		EmpresaEmisoraDTO emisor, List<LineaFacturaV2Response> lineas) {
}

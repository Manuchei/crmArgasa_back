package com.empresa.crm.dto.facturacionv2;

import java.time.LocalDate;
import java.util.List;

public record FacturaV2Response(Long id, String empresa, String serie, Integer numero, LocalDate fechaEmision,
		String estado, Double baseImponible, Double ivaTotal, Double total, ClienteDTO cliente,
		EmpresaEmisoraDTO emisor, List<LineaFacturaV2Response> lineas, String facturaDirectaCompanyId,
		String facturaDirectaId, String facturaDirectaNumero, String verifactuEstado, String verifactuQrUrl) {

	// Mantiene compatibles las llamadas existentes al constructor.
	public FacturaV2Response(Long id, String empresa, String serie, Integer numero, LocalDate fechaEmision,
			String estado, Double baseImponible, Double ivaTotal, Double total, ClienteDTO cliente,
			EmpresaEmisoraDTO emisor, List<LineaFacturaV2Response> lineas) {

		this(id, empresa, serie, numero, fechaEmision, estado, baseImponible, ivaTotal, total, cliente, emisor, lineas,
				null, null, null, null, null);
	}
}
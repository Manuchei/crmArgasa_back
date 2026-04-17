package com.empresa.crm.dto.facturacionv2;

public record LineaFacturaV2UpdateRequest(Long id, String descripcion, Double cantidad, Double precioUnitario,
		Double descuentoPct, Double ivaPct) {
}
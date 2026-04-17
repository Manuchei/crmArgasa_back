package com.empresa.crm.dto.facturacionv2;

public record LineaFacturaV2Response(Long id, String tipoOrigen, Long origenId, String descripcion, Double cantidad,
		Double precioUnitario, Double descuentoPct, Double subtotal, Double ivaPct, Double totalLinea) {
}
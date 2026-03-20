package com.empresa.crm.dto.facturacionv2;

import java.time.LocalDate;
import java.util.List;

public record ActualizarFacturaV2Request(LocalDate fechaEmision, List<LineaFacturaV2UpdateRequest> lineas) {
}
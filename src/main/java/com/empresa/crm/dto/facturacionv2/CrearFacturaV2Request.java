package com.empresa.crm.dto.facturacionv2;

import java.util.List;

public record CrearFacturaV2Request(

		Long clienteId, String serie, List<Long> servicioId, List<Long> lineasAlbaranIds

) {

}

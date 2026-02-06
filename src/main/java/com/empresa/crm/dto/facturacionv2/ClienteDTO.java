package com.empresa.crm.dto.facturacionv2;

public record ClienteDTO(
    Long id,
    String nombreApellidos,
    String cifDni,
    String direccion,
    String codigoPostal,
    String poblacion,
    String provincia,
    String telefono,
    String email
) {}

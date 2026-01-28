package com.empresa.crm.dto;

import lombok.Data;

@Data
public class RutaDiaItemDTO {
    private String origen;
    private String destino;
    private String tarea;
    private String observaciones;
    private String estado;   // opcional
    private String empresa;  // opcional: si no viene, se usará la del request
}

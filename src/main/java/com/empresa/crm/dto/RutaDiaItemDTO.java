package com.empresa.crm.dto;

import lombok.Data;

@Data
public class RutaDiaItemDTO {

    private Long clienteId;      // ✅ obligatorio
    private String tarea;        // opcional
    private String observaciones;// opcional
    private String estado;       // opcional
    private String empresa;      // opcional (si no viene, usa request.empresa)

    // ❌ ya NO hace falta: origen/destino
}

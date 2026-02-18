package com.empresa.crm.dto;

import java.util.List;
import lombok.Data;

@Data
public class RutaDiaRequestDTO {
    private String fecha; // "yyyy-MM-dd" o "dd/MM/yyyy"
    private String nombreTransportista;
    private String emailTransportista;
    private String estado; // opcional (si no viene, "pendiente")
    private List<RutaDiaItemDTO> rutas;

    private String empresa; // OBLIGATORIA (ARGASA / ELECTROLUGA)
}

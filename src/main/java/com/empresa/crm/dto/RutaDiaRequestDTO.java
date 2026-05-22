package com.empresa.crm.dto;

import java.util.List;
import lombok.Data;

@Data
public class RutaDiaRequestDTO {

    private String fecha;
    private Long transportistaId; // ✅ nuevo
    private String nombreTransportista;
    private String emailTransportista;
    
    private String acompanante1;
    private String acompanante2;
    private String estado;
    private List<RutaDiaItemDTO> rutas;

    private String empresa;
}

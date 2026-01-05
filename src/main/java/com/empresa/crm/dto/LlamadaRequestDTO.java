package com.empresa.crm.dto;

import lombok.Data;

@Data
public class LlamadaRequestDTO {
  private String motivo;
  private String fecha; // yyyy-MM-ddTHH:mm
  private String estado;
  private String observaciones;
  private Long clienteId;
}

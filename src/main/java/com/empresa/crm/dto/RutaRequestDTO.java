package com.empresa.crm.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
public class RutaRequestDTO {

	private Long clienteId;
	private Long transportistaId; // ✅ nuevo
	private String empresa;

	private String nombreTransportista;
	private String emailTransportista;

	private LocalDate fecha;
	private String estado;

	private String origen;
	private String destino;

	private String tarea;
	private String observaciones;

	private List<RutaLineaDTO> lineas;
}
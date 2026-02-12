package com.empresa.crm.dto;

import java.time.LocalDate;

import lombok.Data;

@Data
public class RutaRequestDTO {

	private Long clienteId; // ✅ obligatorio
	private String empresa; // opcional (si no viene, se pilla de header o del cliente)

	private String nombreTransportista;
	private String emailTransportista;

	private LocalDate fecha;
	private String estado;

	private String origen;
	private String destino;

	private String tarea;
	private String observaciones;
}

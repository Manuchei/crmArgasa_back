package com.empresa.crm.dto;

import java.util.List;

import lombok.Data;

@Data
public class RutaDiaRequestDTO {
	
	private String fecha; //"yyy-MM-dd (string para simplificar con Angular
	private String nombreTransportista;
	private String emailTransportista;
	private String estado; //opcional (si no viene, "pendiente")
	private List <RutaDiaItemDTO> rutas;

}

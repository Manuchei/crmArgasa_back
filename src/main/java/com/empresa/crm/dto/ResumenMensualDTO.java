package com.empresa.crm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumenMensualDTO {
	private String empresa;
	private String mes; // formato yyyy-MM
	private int facturasClientes;
	private double totalClientes;
	private double totalClientesPagado;
	private double totalClientesPendiente;
	private int facturasProveedores;
	private double totalProveedores;
	private double totalProveedoresPagado;
	private double totalProveedoresPendiente;
}

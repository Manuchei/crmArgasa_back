package com.empresa.crm.dto;

import java.util.List;

import com.empresa.crm.entities.Cliente;
import com.empresa.crm.entities.Proveedor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultadoBusquedaDTO {
	private List<Cliente> clientes;
	private List<Proveedor> proveedores;
}

package com.empresa.crm.dto;

public class ProductoPendienteDto {

	private Long productoId;
	private String codigo;
	private String nombre;
	private Long pendiente;

	// ✅ ESTE constructor es el que necesita el "select new ..."
	public ProductoPendienteDto(Long productoId, String codigo, String nombre, Long pendiente) {
		this.productoId = productoId;
		this.codigo = codigo;
		this.nombre = nombre;
		this.pendiente = pendiente;
	}

	public Long getProductoId() {
		return productoId;
	}

	public String getCodigo() {
		return codigo;
	}

	public String getNombre() {
		return nombre;
	}

	public Long getPendiente() {
		return pendiente;
	}

	public void setProductoId(Long productoId) {
		this.productoId = productoId;
	}

	public void setCodigo(String codigo) {
		this.codigo = codigo;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public void setPendiente(Long pendiente) {
		this.pendiente = pendiente;
	}
}
package com.empresa.crm.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "inventarios_lineas")
public class InventarioLinea {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long productoId;

	private String referencia;

	private String gama;

	private String marca;

	private String modelo;

	private String familia;

	private String subfamilia;

	private String descripcion;

	private Integer stockSistema = 0;

	private Integer stockContado = 0;

	private Integer diferencia = 0;

	private Double precioUnitario = 0.0;

	private Double precioTotal = 0.0;

	@ManyToOne
	@JoinColumn(name = "inventario_id")
	@JsonBackReference("inventario-lineas")
	private Inventario inventario;

	@PrePersist
	@PreUpdate
	public void recalcular() {

		int sistema = stockSistema == null ? 0 : stockSistema;
		int contado = stockContado == null ? 0 : stockContado;

		this.diferencia = contado - sistema;

		double precio = precioUnitario == null ? 0.0 : precioUnitario;

		this.precioTotal = contado * precio;
	}
}
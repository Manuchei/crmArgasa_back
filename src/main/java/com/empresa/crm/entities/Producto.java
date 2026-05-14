package com.empresa.crm.entities;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import lombok.Data;

@Data
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@Entity
@Table(name = "productos")
public class Producto {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "fecha_alta", nullable = false)
	private LocalDate fechaAlta;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "proveedor_id")
	@JsonBackReference("proveedor-productos")
	private Proveedor proveedor;

	@Column(nullable = false)
	private Integer unidades = 0;

	@Column(nullable = false, unique = true)
	private String referencia;

	@Column(nullable = false)
	private String gama;

	@Column(nullable = false)
	private String marca;

	@Column(nullable = false)
	private String modelo;

	@Column(nullable = false)
	private String familia;

	@Column(nullable = false)
	private String subfamilia;

	@Column(nullable = false, length = 1000)
	private String descripcion;

	@Column(name = "precio_sin_iva", nullable = false)
	private Double precioSinIva = 0.0;

	@Column(nullable = false)
	private String empresa;
}
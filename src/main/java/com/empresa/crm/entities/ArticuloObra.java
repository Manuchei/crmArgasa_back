package com.empresa.crm.entities;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "articulos_obra")
public class ArticuloObra {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "trabajo_id", nullable = false)
	@JsonIgnore
	private Trabajo trabajo;

	@Column(nullable = false)
	private LocalDate fecha;

	@Column(nullable = false, length = 500)
	private String descripcion;

	@Column(nullable = false, precision = 12, scale = 3)
	private BigDecimal cantidad;

	@Column(name = "precio_unitario", nullable = false, precision = 14, scale = 2)
	private BigDecimal precioUnitario;

	@Column(nullable = false, precision = 16, scale = 2)
	private BigDecimal importe;
}
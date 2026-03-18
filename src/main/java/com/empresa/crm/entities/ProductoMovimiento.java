package com.empresa.crm.entities;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "productos_movimientos")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class ProductoMovimiento {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String empresa;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "producto_id", nullable = false)
	private Producto producto;

	@Column(nullable = false)
	private String tipo; // ENTRADA / SALIDA

	@Column(nullable = false)
	private Integer cantidad;

	@Column(name = "stock_anterior", nullable = false)
	private Integer stockAnterior;

	@Column(name = "stock_nuevo", nullable = false)
	private Integer stockNuevo;

	@Column(columnDefinition = "TEXT")
	private String motivo;

	@Column(nullable = false)
	private LocalDateTime fecha;
}
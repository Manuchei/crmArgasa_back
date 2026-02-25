package com.empresa.crm.entities;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "ruta_productos")
public class RutaProducto {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "ruta_id")
	private Ruta ruta;

	@ManyToOne(optional = false)
	@JoinColumn(name = "producto_id")
	private Producto producto;

	@Column(nullable = false)
	private Integer cantidad;
}
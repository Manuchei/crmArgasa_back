package com.empresa.crm.entities;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "ruta_lineas")
public class RutaLinea {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "ruta_id")
	@JsonBackReference
	private Ruta ruta;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "producto_id")
	private Producto producto;

	@Column(nullable = false)
	private Integer cantidad;

	// PENDIENTE / ENTREGADO / INCIDENCIA
	@Column(nullable = false, length = 20)
	private String estado = "PENDIENTE";
	
	private LocalDateTime fechaEntrega;
}

package com.empresa.crm.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Data
@Entity
@Table(name = "registro_actividad")
public class RegistroActividad {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Instant fecha;

	private String actor;

	private String accion;

	private String empresa;

	private Integer estado;

	@Column(length = 2000)
	private String detalle;
}
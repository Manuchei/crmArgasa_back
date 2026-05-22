package com.empresa.crm.entities;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "tareas")
public class Tarea {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "empresa", nullable = false, length = 20)
	private String empresa;

	@Column(nullable = false)
	private String titulo;

	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
	@Column(name = "fecha", nullable = false)
	private LocalDateTime fecha;

	@Column(nullable = false)
	private String estado = "pendiente";

	private String observaciones;
}
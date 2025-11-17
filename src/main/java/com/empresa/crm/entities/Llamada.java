package com.empresa.crm.entities;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "llamadas")
public class Llamada {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String motivo;

	@Column(name = "fecha", nullable = false)
	private LocalDateTime fecha;

	private String estado;

	private String observaciones;

	@ManyToOne
	@JoinColumn(name = "cliente_id")
	private Cliente cliente;
}

package com.empresa.crm.entities;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "pagos_trabajo")
public class PagoTrabajo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(name = "empresa", nullable = false, length = 20)
	private String empresa;

	// Para listar pagos por cliente sin liarnos
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "cliente_id", nullable = false)
	@JsonIgnore
	private Cliente cliente;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "trabajo_id", nullable = false)
	@JsonIgnore
	private Trabajo trabajo;

	@Column(nullable = false)
	private LocalDateTime fecha = LocalDateTime.now();

	@Column(nullable = false)
	private Double importe = 0.0;

	private String metodo; // efectivo, transferencia, etc.
	private String observaciones; // opcional
}

package com.empresa.crm.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "llamadas")
public class Llamada {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String motivo;

	private LocalDateTime fechaHora;

	private String estado; // "pendiente", "realizada", "cancelada"

	@ManyToOne
	@JoinColumn(name = "cliente_id")
	private Cliente cliente;

	private String observaciones;
}
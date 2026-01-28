package com.empresa.crm.entities;

import java.time.LocalDate;

import jakarta.persistence.Column;
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
@Table(name = "rutas")
public class Ruta {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String nombreTransportista;

	private LocalDate fecha;

	private String estado; // pendiente, en_curso, cerrada

	private String observaciones;
	
	@Column(length = 255)
	private String tarea;
	
	private String emailTransportista;

	private String origen;
	private String destino;
	
	@Column(name = "empresa", nullable = false, length = 20)
	private String empresa;
	
	@ManyToOne
	@JoinColumn(name = "transportista_id")
	private Transportista transportista;

}
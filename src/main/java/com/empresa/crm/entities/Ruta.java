package com.empresa.crm.entities;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;
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

	// ✅ NUEVO: cada ruta pertenece a un cliente
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "cliente_id", nullable = false)
	@JsonIgnore // evita bucles JSON; si luego quieres mostrar datos del cliente, lo hacemos con
				// DTO de respuesta
	private Cliente cliente;

	// ✅ para que Angular reciba clienteId en JSON
	@JsonProperty("clienteId")
	public Long getClienteId() {
		return (cliente != null) ? cliente.getId() : null;
	}
}

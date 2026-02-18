package com.empresa.crm.entities;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;

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

	// ✅ ya no es necesario pedirlos desde el front, pero los mantenemos por
	// compatibilidad
	private String origen;

	private String destino;

	@Column(name = "empresa", nullable = false, length = 20)
	private String empresa;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "transportista_id")
	@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
	private Transportista transportista;

	// ✅ NUEVO: ruta asignada a cliente
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "cliente_id", nullable = false)
	@JsonIgnoreProperties({ "trabajos", "albaranes", "pagos", "hibernateLazyInitializer", "handler" })
	private Cliente cliente;

	@OneToMany(mappedBy = "ruta", cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonManagedReference
	private List<RutaLinea> lineas = new ArrayList<>();

}

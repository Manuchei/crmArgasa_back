package com.empresa.crm.entities;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;

@Data
@Entity
@Table(name = "llamadas")
public class Llamada {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String motivo;

	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")   // 👈 FORMATO CORRECTO
	@Column(name = "fecha", nullable = false)
	private LocalDateTime fecha;

	private String estado;

	private String observaciones;

	@ManyToOne
	@JoinColumn(name = "cliente_id")
	private Cliente cliente;
}

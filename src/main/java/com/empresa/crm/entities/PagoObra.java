package com.empresa.crm.entities;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "pagos_obra")
public class PagoObra {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "trabajo_id", nullable = false)
	@JsonIgnore
	private Trabajo trabajo;

	@Column(nullable = false)
	private LocalDate fecha;

	@Column(nullable = false, precision = 16, scale = 2)
	private BigDecimal importe;

	@Column(length = 500)
	private String concepto;
}
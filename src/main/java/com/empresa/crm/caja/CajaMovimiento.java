package com.empresa.crm.caja;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "caja_movimientos", uniqueConstraints = @UniqueConstraint(columnNames = { "origen", "origen_id" }))
public class CajaMovimiento {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	Long id;

	@Column(name = "caja_id", nullable = false)
	Long cajaId;

	@Column(nullable = false)
	LocalDateTime fecha;

	@Column(nullable = false, length = 30)
	String metodo;

	@Column(nullable = false, precision = 16, scale = 2)
	BigDecimal importe;

	@Column(nullable = false, length = 1000)
	String concepto;

	@Column(nullable = false, length = 255)
	String actor;

	@Column(nullable = false, length = 40)
	String origen;

	@Column(name = "origen_id", nullable = false)
	Long origenId;
}
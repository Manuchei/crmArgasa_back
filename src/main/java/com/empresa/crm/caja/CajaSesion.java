package com.empresa.crm.caja;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "caja_sesiones")
public class CajaSesion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	Long id;

	@Column(nullable = false, length = 20)
	String empresa;

	@Column(nullable = false)
	LocalDateTime apertura;

	LocalDateTime cierre;

	@Column(name = "abierta_por", nullable = false, length = 255)
	String abiertaPor;

	@Column(name = "cerrada_por", length = 255)
	String cerradaPor;

	@Column(name = "saldo_inicial", nullable = false, precision = 16, scale = 2)
	BigDecimal saldoInicial;

	@Column(name = "efectivo_contado", precision = 16, scale = 2)
	BigDecimal efectivoContado;

	@Column(name = "total_efectivo", precision = 16, scale = 2)
	BigDecimal totalEfectivo;

	@Column(name = "total_tpv", precision = 16, scale = 2)
	BigDecimal totalTpv;

	@Column(name = "total_transferencia", precision = 16, scale = 2)
	BigDecimal totalTransferencia;

	@Column(name = "total_otros", precision = 16, scale = 2)
	BigDecimal totalOtros;

	@Column(length = 500)
	String observaciones;
}
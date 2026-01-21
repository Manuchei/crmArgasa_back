package com.empresa.crm.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "lineas_albaran_cliente")
public class LineaAlbaranCliente {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// Opcional (como “Artículo” en la imagen)
	private String codigo;

	@Column(nullable = false)
	private String descripcion;

	private Double unidades = 1.0;
	private Double precio = 0.0;

	// % descuento (0-100)
	private Double dtoPct = 0.0;

	// Calculados
	private Double baseLinea = 0.0;
	private Double descuentoImporte = 0.0;
	private Double totalLinea = 0.0;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "albaran_id")
	@JsonBackReference
	private AlbaranCliente albaran;

	public void recalcular() {
		double uds = unidades != null ? unidades : 0.0;
		double p = precio != null ? precio : 0.0;
		double base = uds * p;

		double pct = dtoPct != null ? dtoPct : 0.0;
		if (pct < 0)
			pct = 0;
		if (pct > 100)
			pct = 100;

		double dto = base * (pct / 100.0);
		double total = base - dto;

		this.baseLinea = base;
		this.descuentoImporte = dto;
		this.totalLinea = total;
	}
}

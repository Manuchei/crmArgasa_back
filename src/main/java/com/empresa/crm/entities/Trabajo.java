package com.empresa.crm.entities;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
@Entity
@Table(name = "trabajos")
public class Trabajo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String descripcion;

	@Column(name = "producto_id")
	private Long productoId;

	private Integer unidades = 1;

	@Column(name = "precio_unitario")
	private Double precioUnitario = 0.0;

	private Double descuento = 0.0;

	private Double importe = 0.0;

	private Double importePagado = 0.0;

	private boolean pagado = false;

	// ✅ NUEVO (ya lo tienes en BD)
	@Column(nullable = false)
	private boolean entregado = false;

	// ✅ NUEVO (ya lo tienes en BD)
	@Column(name = "fecha_entrega")
	private LocalDateTime fechaEntrega;

	@Column(name = "empresa", nullable = false, length = 20)
	private String empresa;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "cliente_id")
	@JsonBackReference("cliente-trabajos")
	private Cliente cliente;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "proveedor_id")
	@JsonIgnore
	private Proveedor proveedor;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "factura_id")
	@JsonIgnore
	private FacturaProveedor factura;

	@PrePersist
	@PreUpdate
	private void normalizarYCalcularImporte() {

		if (unidades == null || unidades <= 0)
			unidades = 1;
		if (precioUnitario == null || precioUnitario < 0)
			precioUnitario = 0.0;

		if (descuento == null)
			descuento = 0.0;
		if (descuento < 0)
			descuento = 0.0;
		if (descuento > 100)
			descuento = 100.0;

		if (importePagado == null)
			importePagado = 0.0;
		if (importePagado < 0)
			importePagado = 0.0;

		double bruto = unidades * precioUnitario;
		double factor = 1 - (descuento / 100.0);
		double neto = bruto * factor;

		neto = Math.round(neto * 100.0) / 100.0;
		this.importe = neto;

		this.pagado = this.importePagado >= this.importe;
	}
}
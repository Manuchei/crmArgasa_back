package com.empresa.crm.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "lineas_factura_proveedor")
public class LineaFacturaProveedor {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "factura_id", nullable = false)
	@JsonIgnoreProperties({ "lineas", "proveedor", "albaranProveedor", "hibernateLazyInitializer", "handler" })
	private FacturaProveedor factura;

	@Column(name = "tipo_origen")
	private String tipoOrigen; // PRODUCTO | TRABAJO | ALBARAN_LINEA

	@Column(name = "origen_id")
	private Long origenId;

	@Column(nullable = false)
	private String descripcion;

	@Column(nullable = false)
	private Double cantidad = 0.0;

	@Column(name = "precio_unitario", nullable = false)
	private Double precioUnitario = 0.0;

	@Column(name = "descuento_pct", nullable = false)
	private Double descuentoPct = 0.0;

	@Column(nullable = false)
	private Double subtotal = 0.0;

	@Column(name = "iva_pct", nullable = false)
	private Double ivaPct = 0.0;

	@Column(name = "total_linea", nullable = false)
	private Double totalLinea = 0.0;

	public void recalcular() {
		double cantidadSafe = cantidad != null ? cantidad : 0.0;
		double precioSafe = precioUnitario != null ? precioUnitario : 0.0;
		double descuentoPctSafe = descuentoPct != null ? descuentoPct : 0.0;
		double ivaPctSafe = ivaPct != null ? ivaPct : 0.0;

		if (descuentoPctSafe < 0)
			descuentoPctSafe = 0;
		if (descuentoPctSafe > 100)
			descuentoPctSafe = 100;
		if (ivaPctSafe < 0)
			ivaPctSafe = 0;

		double baseBruta = cantidadSafe * precioSafe;
		double descuentoImporte = baseBruta * (descuentoPctSafe / 100.0);
		double subtotalCalculado = baseBruta - descuentoImporte;
		double totalCalculado = subtotalCalculado + (subtotalCalculado * ivaPctSafe / 100.0);

		this.subtotal = subtotalCalculado;
		this.totalLinea = totalCalculado;
	}
}
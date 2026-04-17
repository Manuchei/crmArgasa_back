package com.empresa.crm.entities.facturacionV2;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "lineas_factura_v2")
public class LineaFacturaV2 {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "factura_id")
	private FacturaV2 factura;

	@Column(name = "tipo_origen", nullable = false)
	private String tipoOrigen; // SERVICIO | ALBARAN_LINEA

	@Column(name = "origen_id", nullable = false)
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

		double bruto = cantidadSafe * precioSafe;
		double descuento = bruto * (descuentoPctSafe / 100.0);
		double subtotalCalculado = bruto - descuento;
		double totalCalculado = subtotalCalculado + (subtotalCalculado * ivaPctSafe / 100.0);

		this.subtotal = round2(subtotalCalculado);
		this.totalLinea = round2(totalCalculado);
	}

	private double round2(double v) {
		return Math.round(v * 100.0) / 100.0;
	}
}
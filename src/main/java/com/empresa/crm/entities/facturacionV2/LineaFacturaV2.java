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
	private Double cantidad;

	@Column(name = "precio_unitario", nullable = false)
	private Double precioUnitario;

	@Column(nullable = false)
	private Double subtotal;

	@Column(name = "iva_pct", nullable = false)
	private Double ivaPct;

	@Column(name = "total_linea", nullable = false)
	private Double totalLinea;

	// getters/setters
}

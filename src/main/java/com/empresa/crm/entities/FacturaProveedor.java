package com.empresa.crm.entities;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "facturas_proveedores")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class FacturaProveedor {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String empresa;
	private LocalDate fechaEmision;

	@Column(nullable = false)
	private String estado = "BORRADOR"; // BORRADOR | EMITIDA | PAGADA

	@Column(name = "base_imponible", nullable = false)
	private Double baseImponible = 0.0;

	@Column(name = "iva_total", nullable = false)
	private Double ivaTotal = 0.0;

	@Column(name = "total_importe", nullable = false)
	private Double totalImporte = 0.0;

	private boolean pagada = false;

	@Column(name = "numero_interno", unique = true)
	private String numeroInterno;

	@Column(name = "numero_factura_proveedor")
	private String numeroFacturaProveedor;

	@ManyToOne
	@JoinColumn(name = "proveedor_id")
	@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler", "productos", "albaranes" })
	private Proveedor proveedor;

	@ManyToOne
	@JoinColumn(name = "albaran_proveedor_id")
	@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler", "proveedor" })
	private AlbaranProveedor albaranProveedor;

	@OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<LineaFacturaProveedor> lineas = new ArrayList<>();

	public void recalcularTotales() {
		double base = 0.0;
		double iva = 0.0;
		double total = 0.0;

		if (lineas != null) {
			for (LineaFacturaProveedor linea : lineas) {
				if (linea == null)
					continue;
				linea.recalcular();

				double subtotalLinea = linea.getSubtotal() != null ? linea.getSubtotal() : 0.0;
				double totalLinea = linea.getTotalLinea() != null ? linea.getTotalLinea() : 0.0;

				base += subtotalLinea;
				total += totalLinea;
				iva += (totalLinea - subtotalLinea);
			}
		}

		this.baseImponible = base;
		this.ivaTotal = iva;
		this.totalImporte = total;
		this.pagada = "PAGADA".equalsIgnoreCase(this.estado);
	}
}
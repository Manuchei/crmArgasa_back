package com.empresa.crm.entities;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Data
@Entity
@Table(name = "trabajos")
public class Trabajo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String descripcion;
	private Double importe = 0.0;
	private Double importePagado = 0.0;
	private boolean pagado = false;

	// 🔹 Relación con Cliente
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "cliente_id")
	@JsonBackReference
	private Cliente cliente;

	// 🔹 Relación con Proveedor (para facturas)
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "proveedor_id")
	private Proveedor proveedor;

	// 🔹 Relación con FacturaProveedor
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "factura_id")
	private FacturaProveedor factura;

	// Métodos adicionales usados en otros servicios
	public boolean isPagado() {
		return pagado;
	}

	public void setPagado(boolean pagado) {
		this.pagado = pagado;
	}

	public FacturaProveedor getFactura() {
		return factura;
	}

	public void setFactura(FacturaProveedor factura) {
		this.factura = factura;
	}

	public Proveedor getProveedor() {
		return proveedor;
	}

	public void setProveedor(Proveedor proveedor) {
		this.proveedor = proveedor;
	}
}

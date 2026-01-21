package com.empresa.crm.entities;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

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
@Table(name = "albaranes_clientes")
public class AlbaranCliente {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// Argasa / Luga
	@Column(nullable = false)
	private String empresa;

	@Column(nullable = false)
	private LocalDate fechaEmision = LocalDate.now();

	// Opcional: número visible (si quieres tipo A-000012)
	private String numero;

	// ====== SNAPSHOT CLIENTE (foto en el momento del albarán) ======
	private String nombreApellidos;
	private String nombreComercial;

	private String direccion;
	private String codigoPostal;
	private String poblacion;
	private String provincia;

	private String telefono;
	private String movil;
	private String cifDni;
	private String email;

	// ====== Campos extra (opcionales) ======
	private String modoEnvio;
	private String formaCobro;
	private String cuenta;
	private String observaciones;

	// ====== Totales ======
	private Double subtotal = 0.0;
	private Double totalDescuento = 0.0;
	private Double totalImporte = 0.0;

	// Estado
	private boolean confirmado = false;

	@ManyToOne
	@JoinColumn(name = "cliente_id")
	private Cliente cliente;

	@OneToMany(mappedBy = "albaran", cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonManagedReference
	private List<LineaAlbaranCliente> lineas = new ArrayList<>();

	// ============================
	// Helpers
	// ============================
	public void recalcularTotales() {
		double sub = 0.0;
		double dto = 0.0;
		double total = 0.0;

		if (lineas != null) {
			for (LineaAlbaranCliente l : lineas) {
				if (l == null)
					continue;
				l.recalcular();
				sub += safe(l.getBaseLinea());
				dto += safe(l.getDescuentoImporte());
				total += safe(l.getTotalLinea());
			}
		}

		this.subtotal = sub;
		this.totalDescuento = dto;
		this.totalImporte = total;
	}

	private double safe(Double v) {
		return v != null ? v : 0.0;
	}
}

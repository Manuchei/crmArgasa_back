package com.empresa.crm.entities;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "albaranes_proveedores")
public class AlbaranProveedor {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String empresa;

	@Column(nullable = false)
	private LocalDate fechaEmision = LocalDate.now();

	// Número interno del sistema
	private String numeroInterno;

	// Número que mete el usuario del proveedor
	private String numeroProveedor;

	// Combinación visual de ambos
	private String numeroGenerado;

	// ===== SNAPSHOT PROVEEDOR =====
	private String nombre;
	private String apellido;
	private String oficio;
	private String cif;
	private String direccion;
	private String codigoPostal;
	private String localidad;
	private String provincia;
	private String pais;
	private String telefono;
	private String email;
	private String contacto;

	@Column(columnDefinition = "TEXT")
	private String datosBancarios;

	@Column(columnDefinition = "TEXT")
	private String notas;

	@Column(columnDefinition = "TEXT")
	private String contactos;

	// ===== TOTALES =====
	private Double subtotal = 0.0;
	private Double totalDescuento = 0.0;
	private Double totalImporte = 0.0;

	private boolean confirmado = false;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "proveedor_id")
	@JsonBackReference("proveedor-albaranes")
	private Proveedor proveedor;

	@OneToMany(mappedBy = "albaran", cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonManagedReference("albaran-proveedor-lineas")
	private List<LineaAlbaranProveedor> lineas = new ArrayList<>();

	public void recalcularTotales() {
		double sub = 0.0;
		double dto = 0.0;
		double total = 0.0;

		if (lineas != null) {
			for (LineaAlbaranProveedor l : lineas) {
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
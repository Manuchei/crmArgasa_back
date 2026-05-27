package com.empresa.crm.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "inventarios")
public class Inventario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 20)
	private String empresa;

	@Column(nullable = false)
	private LocalDateTime fecha;

	@Column(length = 150)
	private String descripcion;

	@Column(length = 100)
	private String realizadoPor;

	private Integer totalUnidades = 0;

	private Double totalInventario = 0.0;

	@OneToMany(mappedBy = "inventario", cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonManagedReference("inventario-lineas")
	private List<InventarioLinea> lineas = new ArrayList<>();

	@PrePersist
	public void prePersist() {
		if (fecha == null) {
			fecha = LocalDateTime.now();
		}
	}

	public void recalcularTotales() {
		int unidades = 0;
		double total = 0.0;

		if (lineas != null) {
			for (InventarioLinea linea : lineas) {
				linea.recalcular();
				unidades += linea.getStockContado() == null ? 0 : linea.getStockContado();
				total += linea.getPrecioTotal() == null ? 0.0 : linea.getPrecioTotal();
			}
		}

		this.totalUnidades = unidades;
		this.totalInventario = total;
	}
}
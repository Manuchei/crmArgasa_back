package com.empresa.crm.entities;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "trabajos")
@NoArgsConstructor
@AllArgsConstructor
public class Trabajo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String descripcion;
	private LocalDate fecha;
	private Double importe;
	private boolean pagado;

	@ManyToOne
	@JoinColumn(name = "cliente_id")
	@JsonBackReference
	private Cliente cliente;


	@ManyToOne
	@JoinColumn(name = "proveedor_id")
	private Proveedor proveedor;

	@ManyToOne
	@JoinColumn(name = "factura_id")
	private FacturaProveedor factura;
}
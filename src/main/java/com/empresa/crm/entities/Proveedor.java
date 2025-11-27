package com.empresa.crm.entities;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "proveedores")
public class Proveedor {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String nombre;
	private String apellido;
	private String oficio; // fontanero, electricista, carpintero...

	@Column(nullable = true)
	private String empresa; // 🔹 "argasa" o "luga"

	private String telefono;
	private String email;

	private boolean trabajaEnArgasa;
	private boolean trabajaEnLuga;

	private String trabajoRealizado;

	@OneToMany(mappedBy = "proveedor", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JsonIgnore   // ✅ en vez de JsonManagedReference
	private List<Trabajo> trabajos;

	@OneToMany(mappedBy = "proveedor", cascade = CascadeType.ALL)
	private List<FacturaProveedor> facturas;

	@Column(name = "importe_total")
	private Double importeTotal;

	@Column(name = "importe_pagado")
	private Double importePagado;

	@Column(name = "importe_pendiente")
	private Double importePendiente;

}
package com.empresa.crm.entities;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.*;
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
	private String oficio;

	@Column(nullable = true)
	private String empresa;

	private String telefono;
	private String email;

	private boolean trabajaEnArgasa;
	private boolean trabajaEnLuga;

	private String trabajoRealizado;

	// NUEVOS CAMPOS
	private String direccion;
	private String cif;

	@Column(name = "fecha_alta_proveedor")
	private LocalDate fechaAltaProveedor;

	private String localidad;

	@Column(name = "codigo_postal")
	private String codigoPostal;

	private String provincia;
	private String pais;
	private String contacto;

	@Column(name = "datos_bancarios", columnDefinition = "TEXT")
	private String datosBancarios;

	@Column(columnDefinition = "TEXT")
	private String notas;

	@Column(columnDefinition = "TEXT")
	private String contactos;

	@OneToMany(mappedBy = "proveedor", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JsonIgnore
	private List<Trabajo> trabajos;

	@OneToMany(mappedBy = "proveedor", cascade = CascadeType.ALL)
	@JsonIgnore
	private List<FacturaProveedor> facturas;

	@OneToMany(mappedBy = "proveedor", cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonManagedReference("proveedor-productos")
	private List<Producto> productos = new ArrayList<>();

	@Column(name = "importe_total")
	private Double importeTotal;

	@Column(name = "importe_pagado")
	private Double importePagado;

	@Column(name = "importe_pendiente")
	private Double importePendiente;
}
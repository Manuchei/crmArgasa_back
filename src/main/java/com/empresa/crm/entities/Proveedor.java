package com.empresa.crm.entities;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

	@OneToMany(mappedBy = "proveedor", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Trabajo> trabajos = new ArrayList<>();

	@OneToMany(mappedBy = "proveedor", cascade = CascadeType.ALL)
	private List<FacturaProveedor> facturas;

}
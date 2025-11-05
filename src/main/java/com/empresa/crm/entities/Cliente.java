package com.empresa.crm.entities;

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
@Table(name = "clientes")
public class Cliente {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String nombre;
	private String apellido;
	private String empresa; // 🔹 "argasa" o "luga"
	private String telefono;
	private String email;

	@Column(name = "saldo_debe")
	private Double saldoDebe;

	@Column(name = "saldo_pagado")
	private Double saldoPagado;

	@OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL)
	private List<ServicioCliente> servicios;

	@OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL)
	private List<FacturaCliente> facturas;
}
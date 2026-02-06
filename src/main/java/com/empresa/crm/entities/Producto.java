package com.empresa.crm.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "productos")
public class Producto {
	
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(nullable = false, unique = true)
	private String codigo;
	
	@Column(nullable = false)
	private String nombre;
	
	@Column(nullable = false)
	private int stock;
	
	@Column(nullable = false)
	private String empresa; //"ARGASA" O "ELECTROLUGA"

}

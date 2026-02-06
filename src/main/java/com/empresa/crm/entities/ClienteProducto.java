package com.empresa.crm.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "cliente_producto")
public class ClienteProducto {
	
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@ManyToOne(optional = false)
	@JoinColumn(name = "cliente_id")
	private Cliente cliente;
	
	@ManyToOne(optional = false)
	@JoinColumn(name = "producto_id")
	private Producto producto;
	
	@Column(nullable = false)
	private LocalDateTime fecha = LocalDateTime.now();

}

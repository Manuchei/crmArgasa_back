package com.empresa.crm.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Data
@Entity
@Table(name = "cliente_producto", uniqueConstraints = {
		@UniqueConstraint(name = "uk_cp_empresa_cliente_producto", columnNames = { "empresa", "cliente_id",
				"producto_id" }) }, indexes = { @Index(name = "idx_cp_empresa", columnList = "empresa"),
						@Index(name = "idx_cp_cliente", columnList = "cliente_id"),
						@Index(name = "idx_cp_producto", columnList = "producto_id") })
public class ClienteProducto {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// ✅ Empresa/Tenant (ARGASA/ELECTROLUGA)
	@Column(nullable = false, length = 30)
	private String empresa;

	@ManyToOne(optional = false)
	@JoinColumn(name = "cliente_id")
	private Cliente cliente;

	@ManyToOne(optional = false)
	@JoinColumn(name = "producto_id")
	private Producto producto;

	@Column(nullable = false)
	private LocalDateTime fecha = LocalDateTime.now();

	@Column(nullable = false, length = 20)
	private String estado = "PENDIENTE";

	private LocalDateTime fechaEntrega; // cuando pasa a ENTREGADO

	@Column(nullable = false)
	private Integer cantidadTotal = 0;

	@Column(nullable = false)
	private Integer cantidadEntregada = 0;

	@Column(nullable = false)
	private boolean entregado = false;
}
package com.empresa.crm.entities.facturacionV2;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.empresa.crm.entities.Cliente;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "factura_v2", uniqueConstraints = @UniqueConstraint(columnNames = { "empresa", "serie", "numero" }))
public class FacturaV2 {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "cliente_id", nullable = false)
	private Cliente cliente;

	@Column(name = "empresa", nullable = false)
	private String empresa;

	@Column(nullable = false)
	private String serie;

	@Column(nullable = false)
	private Integer numero;

	@Column(name = "fecha_emision", nullable = false)
	private LocalDate fechaEmision;

	@Column(nullable = false)
	private String estado; // BORRADOR / EMITIDA / PAGADA / ANULADA

	@Column(name = "base_imponible", nullable = false)
	private Double baseImponible = 0.0;

	@Column(name = "iva_total", nullable = false)
	private Double ivaTotal = 0.0;

	@Column(nullable = false)
	private Double total = 0.0;

	@Column(name = "hash_emision")
	private String hashEmision;

	@OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<LineaFacturaV2> lineas = new ArrayList<>();
}

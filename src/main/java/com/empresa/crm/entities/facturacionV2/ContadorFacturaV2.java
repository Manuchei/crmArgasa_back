package com.empresa.crm.entities.facturacionV2;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "contador_factura_v2", uniqueConstraints = @UniqueConstraint(columnNames = { "empresa", "serie" }))
public class ContadorFacturaV2 {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String empresa;

	@Column(nullable = false)
	private String serie;

	@Column(name = "siguiente_numero", nullable = false)
	private Integer siguienteNumero = 1;

	// getters/setters
}

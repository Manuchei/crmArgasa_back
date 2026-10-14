package com.empresa.crm.fichajes;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "jornadas_fichaje")
public class JornadaFichaje {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	Long id;
	@Column(name = "empleado_id", nullable = false)
	Long empleadoId;
	@Column(nullable = false)
	LocalDateTime entrada;
	LocalDateTime salida;
	@Column(name = "entrada_original", nullable = false)
	LocalDateTime entradaOriginal;
	@Column(name = "salida_original")
	LocalDateTime salidaOriginal;
	@Column(name = "clave_entrada", nullable = false, unique = true, length = 36)
	String claveEntrada;
	@Column(name = "clave_salida", unique = true, length = 36)
	String claveSalida;
	@Version
	@Column(nullable = false)
	Long version;
}

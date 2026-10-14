package com.empresa.crm.fichajes;

import jakarta.persistence.*;

@Entity
@Table(name = "empleados_fichaje")
public class EmpleadoFichaje {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	Long id;
	@Column(name = "usuario_id", nullable = false, unique = true)
	Long usuarioId;
	@Column(nullable = false)
	boolean activo = true;
}

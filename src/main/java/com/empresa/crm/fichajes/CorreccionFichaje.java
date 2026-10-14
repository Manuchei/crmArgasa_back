package com.empresa.crm.fichajes;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "correcciones_fichaje")
public class CorreccionFichaje {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	Long id;
	@Column(name = "jornada_id", nullable = false)
	Long jornadaId;
	@Column(name = "actor_usuario_id", nullable = false)
	Long actorUsuarioId;
	@Column(nullable = false)
	LocalDateTime fecha;
	@Column(nullable = false, length = 500)
	String motivo;
	@Column(name = "entrada_anterior", nullable = false)
	LocalDateTime entradaAnterior;
	@Column(name = "salida_anterior")
	LocalDateTime salidaAnterior;
	@Column(name = "entrada_nueva", nullable = false)
	LocalDateTime entradaNueva;
	@Column(name = "salida_nueva")
	LocalDateTime salidaNueva;
}

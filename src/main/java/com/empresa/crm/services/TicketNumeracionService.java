package com.empresa.crm.services;

import java.time.Year;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketNumeracionService {

	private final JdbcTemplate jdbc;

	public TicketNumeracionService(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Transactional
	public String reservar(String empresa, UUID solicitud) {
		int ejercicio = Year.now(ZoneId.of("Europe/Madrid")).getValue();
		String clave = solicitud.toString();

		jdbc.update("""
				INSERT INTO ticket_contador (empresa, ejercicio, ultimo)
				VALUES (?, ?, 0)
				ON DUPLICATE KEY UPDATE ultimo = ultimo
				""", empresa, ejercicio);

		Long ultimo = jdbc.queryForObject("""
				SELECT ultimo
				FROM ticket_contador
				WHERE empresa = ? AND ejercicio = ?
				FOR UPDATE
				""", Long.class, empresa, ejercicio);

		List<Long> existente = jdbc.query("""
				SELECT numero
				FROM ticket_numeracion
				WHERE empresa = ? AND ejercicio = ? AND solicitud = ?
				""", (rs, fila) -> rs.getLong("numero"), empresa, ejercicio, clave);

		long numero;

		if (!existente.isEmpty()) {
			numero = existente.get(0);
		} else {
			numero = Math.addExact(ultimo, 1L);

			jdbc.update("""
					UPDATE ticket_contador
					SET ultimo = ?
					WHERE empresa = ? AND ejercicio = ?
					""", numero, empresa, ejercicio);

			jdbc.update("""
					INSERT INTO ticket_numeracion
					    (solicitud, empresa, ejercicio, numero)
					VALUES (?, ?, ?, ?)
					""", clave, empresa, ejercicio, numero);
		}

		String prefijo = empresa.equals("ARGASA") ? "ARG" : "LUG";

		return "T-" + prefijo + "-" + ejercicio + "-" + String.format("%06d", numero);
	}
}
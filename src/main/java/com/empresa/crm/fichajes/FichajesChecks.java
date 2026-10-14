package com.empresa.crm.fichajes;

import java.lang.reflect.*;
import java.time.*;
import java.util.*;
import java.util.function.*;
import jakarta.persistence.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Checks de servicio con persistencia simulada; no sustituye una prueba MySQL.
 */
public class FichajesChecks {
	static class Memory implements InvocationHandler {
		Map<Long, EmpleadoFichaje> empleados = new HashMap<>();
		Map<Long, JornadaFichaje> jornadas = new HashMap<>();
		List<CorreccionFichaje> cambios = new ArrayList<>();
		long next = 1;
		EntityManager manager = (EntityManager) Proxy.newProxyInstance(EntityManager.class.getClassLoader(),
				new Class[] { EntityManager.class }, this);

		public Object invoke(Object p, Method m, Object[] a) {
			switch (m.getName()) {
			case "createQuery":
			case "createNativeQuery":
				return Proxy.newProxyInstance(TypedQuery.class.getClassLoader(), new Class[] { TypedQuery.class },
						new QueryMemory((String) a[0], this));
			case "find":
				return a[0] == EmpleadoFichaje.class ? empleados.get(a[1]) : jornadas.get(a[1]);
			case "persist":
				if (a[0] instanceof EmpleadoFichaje e) {
					e.id = next++;
					empleados.put(e.id, e);
				}
				if (a[0] instanceof JornadaFichaje j) {
					j.id = next++;
					j.version = 0L;
					jornadas.put(j.id, j);
				}
				if (a[0] instanceof CorreccionFichaje c) {
					c.id = next++;
					cambios.add(c);
				}
				return null;
			case "flush":
			case "refresh":
				return null;
			default:
				throw new UnsupportedOperationException(m.getName());
			}
		}
	}

	static class QueryMemory implements InvocationHandler {
		String sql;
		Memory db;
		Map<String, Object> args = new HashMap<>();
		int first = 0, max = Integer.MAX_VALUE;

		QueryMemory(String sql, Memory db) {
			this.sql = sql;
			this.db = db;
		}

		public Object invoke(Object p, Method m, Object[] a) {
			switch (m.getName()) {
			case "setParameter":
				args.put((String) a[0], a[1]);
				return p;
			case "setLockMode":
				return p;
			case "setFirstResult":
				first = (Integer) a[0];
				return p;
			case "setMaxResults":
				max = (Integer) a[0];
				return p;
			case "getResultList":
				return rows();
			case "getSingleResult":
				return (long) rows().size();
			default:
				throw new UnsupportedOperationException(m.getName());
			}
		}

		List<?> rows() {
			if (sql.startsWith("select id from usuarios where email"))
				return switch ((String) args.get("email")) {
				case "a@test" -> List.of(1L);
				case "b@test" -> List.of(2L);
				default -> List.of();
				};
			if (sql.startsWith("select nombre,email"))
				return List.<Object[]>of(
						new Object[] { "Empleado " + args.get("id"), args.get("id").equals(1L) ? "a@test" : "b@test" });
			if (sql.startsWith("select id from usuarios"))
				return List.of(args.get("id"));
			if (sql.contains("from EmpleadoFichaje"))
				return db.empleados.values().stream()
						.filter(e -> !args.containsKey("usuario") || e.usuarioId.equals(args.get("usuario")))
						.filter(e -> !args.containsKey("id") || e.usuarioId.equals(args.get("id"))).toList();
			if (sql.contains("from CorreccionFichaje"))
				return db.cambios.stream().filter(c -> c.jornadaId.equals(args.get("id"))).toList();
			if (sql.contains("from JornadaFichaje")) {
				return db.jornadas.values().stream()
						.filter(j -> !args.containsKey("e") || j.empleadoId.equals(args.get("e")))
						.filter(j -> !sql.contains("j.salida is null") || sql.contains(" or ") || j.salida == null)
						.filter(j -> !sql.contains("j.claveEntrada=:c") || j.claveEntrada.equals(args.get("c")))
						.filter(j -> !sql.contains("j.claveSalida=:c") || Objects.equals(j.claveSalida, args.get("c")))
						.filter(j -> !args.containsKey("entrada") || j.salida == null
								|| j.salida.isAfter((LocalDateTime) args.get("entrada")))
						.filter(j -> !args.containsKey("salida")
								|| j.entrada.isBefore((LocalDateTime) args.get("salida")))
						.filter(j -> !args.containsKey("excluir") || !j.id.equals(args.get("excluir")))
						.filter(j -> !args.containsKey("inicio") || j.salida == null
								|| !j.salida.isBefore((LocalDateTime) args.get("inicio")))
						.filter(j -> !args.containsKey("fin") || j.entrada.isBefore((LocalDateTime) args.get("fin")))
						.sorted(Comparator.comparing((JornadaFichaje j) -> j.entrada).reversed()).skip(first).limit(max)
						.toList();
			}
			throw new UnsupportedOperationException(sql);
		}
	}

	static void check(boolean ok, String label) {
		if (!ok)
			throw new AssertionError(label);
	}

	static void reject(int status, Runnable fn, String label) {
		try {
			fn.run();
			throw new AssertionError(label);
		} catch (ResponseStatusException e) {
			check(e.getStatusCode().value() == status, label);
		}
	}

	static String key() {
		return UUID.randomUUID().toString();
	}

	static FichajesService service(Memory m, String instant) {
		FichajesService s = new FichajesService(Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
		s.em = m.manager;
		return s;
	}

	public static void main(String[] args) {
		Memory m = new Memory();
		FichajesService s = service(m, "2026-10-13T06:00:00Z");
		var a = s.alta(1L);
		var b = s.alta(2L);
		reject(409, () -> s.alta(1L), "alta duplicada");
		String entradaKey = key();
		var j = s.entrada("a@test", entradaKey);
		check(s.entrada("a@test", entradaKey).id().equals(j.id()), "reintento entrada idempotente");
		reject(409, () -> s.entrada("a@test", key()), "doble entrada");
		check(s.miEstado("b@test").abierta() == null, "aislamiento entre empleados");
		reject(409, () -> s.activar(a.id(), false), "desactivar con jornada abierta");
		var fin = service(m, "2026-10-13T14:00:00Z");
		String salidaKey = key();
		var cerrada = fin.salida("a@test", salidaKey);
		check(cerrada.segundos() == 28800, "duración de 8 horas");
		check(fin.salida("a@test", salidaKey).id().equals(j.id()), "reintento salida idempotente");
		reject(409, () -> fin.salida("a@test", key()), "salida sin entrada");
		fin.activar(b.id(), false);
		reject(409, () -> fin.entrada("b@test", key()), "empleado inactivo");
		fin.activar(b.id(), true);
		fin.corregir(j.id(), Instant.parse("2026-10-13T06:30:00Z"), Instant.parse("2026-10-13T14:00:00Z"), "Olvido", 0L,
				"b@test");
		check(m.cambios.size() == 1, "auditoría creada");
		check(m.jornadas.get(j.id()).entradaOriginal.equals(LocalDateTime.parse("2026-10-13T06:00:00")),
				"original preservado");
		check(fin.correcciones(j.id(), "a@test", false).size() == 1, "historial propio");
		reject(403, () -> fin.correcciones(j.id(), "b@test", false), "historial ajeno bloqueado");
		m.jornadas.get(j.id()).version = 1L;
		reject(409, () -> fin.corregir(j.id(), Instant.parse("2026-10-13T06:00:00Z"), null, "Cambio", 0L, "a@test"),
				"versión desactualizada");
		var siguiente = service(m, "2026-10-13T15:00:00Z");
		siguiente.entrada("a@test", key());
		reject(409,
				() -> siguiente.corregir(j.id(), Instant.parse("2026-10-13T06:00:00Z"), null, "Reabrir", 1L, "a@test"),
				"solape de jornadas");
		var dia = fin.historialPropio("a@test", LocalDate.parse("2026-10-13"), LocalDate.parse("2026-10-13"), 0);
		check(dia.total() == 2, "consulta por fecha");
		check(fin.historialPropio("b@test", LocalDate.parse("2026-10-13"), LocalDate.parse("2026-10-13"), 0)
				.total() == 0, "consulta propia aislada");
		reject(400, () -> fin.historial(null, LocalDate.parse("2026-10-14"), LocalDate.parse("2026-10-13"), 0),
				"intervalo invertido");
		var nocturna = service(m, "2026-10-13T22:30:00Z");
		nocturna.entrada("b@test", key());
		var madrugada = service(m, "2026-10-14T02:00:00Z");
		madrugada.salida("b@test", key());
		check(madrugada.historialPropio("b@test", LocalDate.parse("2026-10-14"), LocalDate.parse("2026-10-14"), 0)
				.total() == 1, "jornada nocturna en día español");
		Memory dst = new Memory();
		var dstEntrada = service(dst, "2026-10-25T00:30:00Z");
		dstEntrada.alta(1L);
		dstEntrada.entrada("a@test", key());
		var dstSalida = service(dst, "2026-10-25T02:30:00Z");
		check(dstSalida.salida("a@test", key()).segundos() == 7200, "duración real en cambio horario");
		System.out.println(
				"Comprobaciones de servicio superadas: sesión, duplicados, reintentos, estados, auditoría, solapes, fechas y cambio horario.");
	}
}

package com.empresa.crm.fichajes;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import jakarta.persistence.*;

@Service
@Transactional
public class FichajesService {
	@PersistenceContext
	EntityManager em;
	private final Clock clock;

	public FichajesService() {
		this(Clock.systemUTC());
	}

	FichajesService(Clock clock) {
		this.clock = clock;
	}

	private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");

	public record EmpleadoDTO(Long id, Long usuarioId, String nombre, String email, boolean activo) {
	}

	public record UsuarioDTO(Long id, String nombre, String email) {
	}

	public record JornadaDTO(Long id, Long empleadoId, String nombre, Instant entrada, Instant salida,
			Instant entradaOriginal, Instant salidaOriginal, Long segundos, Long version) {
	}

	public record CorreccionDTO(Long id, String actor, Instant fecha, String motivo, Instant entradaAnterior,
			Instant salidaAnterior, Instant entradaNueva, Instant salidaNueva) {
	}

	public record MiDTO(EmpleadoDTO empleado, JornadaDTO abierta, Instant horaServidor) {
	}

	public record PaginaDTO(List<JornadaDTO> registros, long total, int pagina, int tamano) {
	}

	private LocalDateTime ahora() {
		return LocalDateTime.ofInstant(clock.instant().truncatedTo(ChronoUnit.SECONDS), ZoneOffset.UTC);
	}

	private Instant instant(LocalDateTime t) {
		return t == null ? null : t.toInstant(ZoneOffset.UTC);
	}

	private ResponseStatusException error(HttpStatus status, String mensaje) {
		return new ResponseStatusException(status, mensaje);
	}

	private Long usuario(String email) {
		List<?> ids = em.createNativeQuery("select id from usuarios where email = :email").setParameter("email", email)
				.getResultList();
		if (ids.size() != 1)
			throw error(HttpStatus.UNAUTHORIZED, "Usuario no disponible");
		return ((Number) ids.get(0)).longValue();
	}

	private Object[] datosUsuario(Long id) {
		List<?> datos = em.createNativeQuery("select nombre,email from usuarios where id = :id").setParameter("id", id)
				.getResultList();
		if (datos.isEmpty())
			throw error(HttpStatus.NOT_FOUND, "Usuario no encontrado");
		return (Object[]) datos.get(0);
	}

	private EmpleadoDTO dto(EmpleadoFichaje e) {
		Object[] u = datosUsuario(e.usuarioId);
		return new EmpleadoDTO(e.id, e.usuarioId, u[0] == null ? String.valueOf(u[1]) : String.valueOf(u[0]),
				String.valueOf(u[1]), e.activo);
	}

	private JornadaDTO dto(JornadaFichaje j) {
		EmpleadoFichaje e = em.find(EmpleadoFichaje.class, j.empleadoId);
		return new JornadaDTO(j.id, j.empleadoId, dto(e).nombre(), instant(j.entrada), instant(j.salida),
				instant(j.entradaOriginal), instant(j.salidaOriginal),
				j.salida == null ? null : Duration.between(j.entrada, j.salida).getSeconds(), j.version);
	}

	private EmpleadoFichaje empleado(Long id) {
		EmpleadoFichaje e = em.find(EmpleadoFichaje.class, id, LockModeType.PESSIMISTIC_WRITE);
		if (e == null)
			throw error(HttpStatus.NOT_FOUND, "Empleado no encontrado");
		return e;
	}

	private EmpleadoFichaje propio(String email) {
		List<EmpleadoFichaje> empleados = em
				.createQuery("select e from EmpleadoFichaje e where e.usuarioId=:usuario", EmpleadoFichaje.class)
				.setParameter("usuario", usuario(email)).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
		if (empleados.isEmpty())
			throw error(HttpStatus.NOT_FOUND, "Un administrador debe darte de alta en Empleados");
		return empleados.get(0);
	}

	private JornadaFichaje abierta(Long empleadoId) {
		List<JornadaFichaje> lista = em
				.createQuery("select j from JornadaFichaje j where j.empleadoId=:e and j.salida is null",
						JornadaFichaje.class)
				.setParameter("e", empleadoId).getResultList();
		return lista.isEmpty() ? null : lista.get(0);
	}

	public MiDTO miEstado(String email) {
		EmpleadoFichaje e = propio(email);
		JornadaFichaje j = abierta(e.id);
		return new MiDTO(dto(e), j == null ? null : dto(j), clock.instant());
	}

	public List<EmpleadoDTO> empleados() {
		return em.createQuery("select e from EmpleadoFichaje e order by e.id", EmpleadoFichaje.class).getResultList()
				.stream().map(this::dto).toList();
	}

	public List<UsuarioDTO> candidatos() {
		List<?> filas = em.createNativeQuery("""
				select u.id,u.nombre,u.email from usuarios u
				where not exists(select 1 from empleados_fichaje e where e.usuario_id=u.id)
				order by u.nombre,u.id
				""").getResultList();
		return filas.stream().map(f -> {
			Object[] a = (Object[]) f;
			return new UsuarioDTO(((Number) a[0]).longValue(),
					a[1] == null ? String.valueOf(a[2]) : String.valueOf(a[1]), String.valueOf(a[2]));
		}).toList();
	}

	public EmpleadoDTO alta(Long usuarioId) {
		if (usuarioId == null)
			throw error(HttpStatus.BAD_REQUEST, "Selecciona un usuario");
		// Serializa altas del mismo usuario y evita duplicar su ficha.
		List<?> usuarios = em.createNativeQuery("select id from usuarios where id=:id for update")
				.setParameter("id", usuarioId).getResultList();
		if (usuarios.isEmpty())
			throw error(HttpStatus.NOT_FOUND, "Usuario no encontrado");
		if (!em.createQuery("select e from EmpleadoFichaje e where e.usuarioId=:id", EmpleadoFichaje.class)
				.setParameter("id", usuarioId).getResultList().isEmpty())
			throw error(HttpStatus.CONFLICT, "Ese usuario ya es empleado");
		EmpleadoFichaje e = new EmpleadoFichaje();
		e.usuarioId = usuarioId;
		em.persist(e);
		em.flush();
		return dto(e);
	}

	public EmpleadoDTO activar(Long id, boolean activo) {
		EmpleadoFichaje e = empleado(id);
		if (!activo && abierta(id) != null)
			throw error(HttpStatus.CONFLICT, "Cierra o corrige su jornada abierta antes de desactivarlo");
		e.activo = activo;
		em.flush();
		return dto(e);
	}

	private String clave(String clave) {
		try {
			if (!UUID.fromString(clave).toString().equals(clave))
				throw new IllegalArgumentException();
		} catch (Exception ex) {
			throw error(HttpStatus.BAD_REQUEST, "Clave de operación no válida");
		}
		return clave;
	}

	public JornadaDTO entrada(String email, String clave) {
		EmpleadoFichaje e = propio(email);
		clave = clave(clave);
		List<JornadaFichaje> repetida = em
				.createQuery("select j from JornadaFichaje j where j.empleadoId=:e and j.claveEntrada=:c",
						JornadaFichaje.class)
				.setParameter("e", e.id).setParameter("c", clave).getResultList();
		if (!repetida.isEmpty())
			return dto(repetida.get(0));
		if (!e.activo)
			throw error(HttpStatus.CONFLICT, "Empleado inactivo");
		if (abierta(e.id) != null)
			throw error(HttpStatus.CONFLICT, "Ya tienes una jornada abierta");
		LocalDateTime ahora = ahora();
		comprobarSolape(e.id, null, ahora, null);
		JornadaFichaje j = new JornadaFichaje();
		j.empleadoId = e.id;
		j.entrada = ahora;
		j.entradaOriginal = ahora;
		j.claveEntrada = clave;
		em.persist(j);
		em.flush();
		return dto(j);
	}

	public JornadaDTO salida(String email, String clave) {
		EmpleadoFichaje e = propio(email);
		clave = clave(clave);
		List<JornadaFichaje> repetida = em
				.createQuery("select j from JornadaFichaje j where j.empleadoId=:e and j.claveSalida=:c",
						JornadaFichaje.class)
				.setParameter("e", e.id).setParameter("c", clave).getResultList();
		if (!repetida.isEmpty())
			return dto(repetida.get(0));
		if (!e.activo)
			throw error(HttpStatus.CONFLICT, "Empleado inactivo");
		JornadaFichaje j = abierta(e.id);
		if (j == null)
			throw error(HttpStatus.CONFLICT, "No tienes una jornada abierta");
		LocalDateTime salida = ahora();
		if (salida.isBefore(j.entrada))
			throw error(HttpStatus.CONFLICT, "La entrada está en el futuro; pide una corrección");
		j.salida = salida;
		j.salidaOriginal = salida;
		j.claveSalida = clave;
		em.flush();
		return dto(j);
	}

	private LocalDateTime inicio(LocalDate d) {
		return LocalDateTime.ofInstant(d.atStartOfDay(MADRID).toInstant(), ZoneOffset.UTC);
	}

	private void rango(LocalDate desde, LocalDate hasta) {
		if (desde == null || hasta == null || hasta.isBefore(desde) || ChronoUnit.DAYS.between(desde, hasta) > 366)
			throw error(HttpStatus.BAD_REQUEST, "Selecciona un intervalo de hasta 367 días");
	}

	public PaginaDTO historialPropio(String email, LocalDate desde, LocalDate hasta, int pagina) {
		return historial(propio(email).id, desde, hasta, pagina);
	}

	public PaginaDTO historial(Long empleadoId, LocalDate desde, LocalDate hasta, int pagina) {
		rango(desde, hasta);
		if (pagina < 0 || pagina > 100000)
			throw error(HttpStatus.BAD_REQUEST, "Página no válida");
		String where = " where j.entrada < :fin and (j.salida is null or j.salida >= :inicio)";
		if (empleadoId != null)
			where += " and j.empleadoId=:e";
		TypedQuery<JornadaFichaje> q = em.createQuery(
				"select j from JornadaFichaje j" + where + " order by j.entrada desc,j.id desc", JornadaFichaje.class);
		TypedQuery<Long> count = em.createQuery("select count(j) from JornadaFichaje j" + where, Long.class);
		for (Query query : List.of(q, count)) {
			query.setParameter("inicio", inicio(desde));
			query.setParameter("fin", inicio(hasta.plusDays(1)));
			if (empleadoId != null)
				query.setParameter("e", empleadoId);
		}
		return new PaginaDTO(
				q.setFirstResult(pagina * 50).setMaxResults(50).getResultList().stream().map(this::dto).toList(),
				count.getSingleResult(), pagina, 50);
	}

	private void comprobarSolape(Long empleadoId, Long excluir, LocalDateTime entrada, LocalDateTime salida) {
		String where = " where j.empleadoId=:e and (j.salida is null or j.salida > :entrada)";
		if (salida != null)
			where += " and j.entrada < :salida";
		if (excluir != null)
			where += " and j.id<>:excluir";
		TypedQuery<Long> q = em.createQuery("select count(j) from JornadaFichaje j" + where, Long.class)
				.setParameter("e", empleadoId).setParameter("entrada", entrada);
		if (salida != null)
			q.setParameter("salida", salida);
		if (excluir != null)
			q.setParameter("excluir", excluir);
		if (q.getSingleResult() > 0)
			throw error(HttpStatus.CONFLICT, "El tramo se solapa con otro fichaje");
	}

	public JornadaDTO corregir(Long id, Instant entrada, Instant salida, String motivo, Long version, String actor) {
		JornadaFichaje previo = em.find(JornadaFichaje.class, id);
		if (previo == null)
			throw error(HttpStatus.NOT_FOUND, "Fichaje no encontrado");
		EmpleadoFichaje empleado = empleado(previo.empleadoId);
		if (!empleado.activo && salida == null)
			throw error(HttpStatus.CONFLICT, "No puedes reabrir la jornada de un empleado inactivo");
		// Refresca después del bloqueo del empleado: detecta otra edición concurrente.
		em.refresh(previo);
		if (version == null || !version.equals(previo.version))
			throw error(HttpStatus.CONFLICT, "El registro cambió. Actualiza la lista antes de corregir");
		if (entrada == null || motivo == null || motivo.isBlank() || motivo.trim().length() > 500)
			throw error(HttpStatus.BAD_REQUEST, "Entrada y motivo de hasta 500 caracteres son obligatorios");
		Instant ahora = clock.instant();
		if (entrada.isAfter(ahora) || (salida != null && (salida.isBefore(entrada) || salida.isAfter(ahora))))
			throw error(HttpStatus.BAD_REQUEST, "Fechas no válidas: salida anterior a entrada o fecha futura");
		LocalDateTime e = LocalDateTime.ofInstant(entrada.truncatedTo(ChronoUnit.SECONDS), ZoneOffset.UTC);
		LocalDateTime s = salida == null ? null
				: LocalDateTime.ofInstant(salida.truncatedTo(ChronoUnit.SECONDS), ZoneOffset.UTC);
		comprobarSolape(previo.empleadoId, id, e, s);
		CorreccionFichaje c = new CorreccionFichaje();
		c.jornadaId = id;
		c.actorUsuarioId = usuario(actor);
		c.fecha = ahora();
		c.motivo = motivo.trim();
		c.entradaAnterior = previo.entrada;
		c.salidaAnterior = previo.salida;
		c.entradaNueva = e;
		c.salidaNueva = s;
		em.persist(c);
		previo.entrada = e;
		previo.salida = s;
		em.flush();
		return dto(previo);
	}

	public List<CorreccionDTO> correcciones(Long id, String email, boolean admin) {
		JornadaFichaje j = em.find(JornadaFichaje.class, id);
		if (j == null)
			throw error(HttpStatus.NOT_FOUND, "Fichaje no encontrado");
		if (!admin && !j.empleadoId.equals(propio(email).id))
			throw error(HttpStatus.FORBIDDEN, "No puedes consultar otro empleado");
		return em
				.createQuery("select c from CorreccionFichaje c where c.jornadaId=:id order by c.id",
						CorreccionFichaje.class)
				.setParameter("id", id).getResultList().stream()
				.map(c -> new CorreccionDTO(c.id, String.valueOf(datosUsuario(c.actorUsuarioId)[1]), instant(c.fecha),
						c.motivo, instant(c.entradaAnterior), instant(c.salidaAnterior), instant(c.entradaNueva),
						instant(c.salidaNueva)))
				.toList();
	}
}

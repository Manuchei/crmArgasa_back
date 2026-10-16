package com.empresa.crm.caja;

import java.math.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

import jakarta.persistence.*;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.empresa.crm.tenant.TenantContext;
import com.empresa.crm.entities.PagoCliente;
import com.empresa.crm.entities.facturacionV2.FacturaV2;

@Service
@Transactional
public class CajaService {

	@PersistenceContext
	EntityManager em;

	private static final BigDecimal ZERO = new BigDecimal("0.00");

	public record Resumen(Long id, String empresa, Instant apertura, Instant cierre, String abiertaPor,
			String cerradaPor, BigDecimal saldoInicial, BigDecimal efectivo, BigDecimal tpv, BigDecimal transferencia,
			BigDecimal otros, BigDecimal totalCobrado, BigDecimal efectivoEsperado, BigDecimal efectivoContado,
			BigDecimal diferencia, String observaciones) {
	}

	public record Movimiento(Long id, Instant fecha, String metodo, BigDecimal importe, String concepto, String actor,
			String origen, Long origenId) {
	}

	public record Detalle(Resumen caja, List<Movimiento> movimientos) {
	}

	public record Historial(List<Resumen> cajas, long total, int pagina) {
	}

	private String empresa() {
		String empresa = TenantContext.get();

		if (!Set.of("ARGASA", "ELECTROLUGA").contains(empresa == null ? "" : empresa)) {
			throw error("Empresa no válida");
		}

		return empresa;
	}

	private ResponseStatusException error(String mensaje) {
		return new ResponseStatusException(HttpStatus.CONFLICT, mensaje);
	}

	private String actor() {
		var authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !authentication.isAuthenticated()) {
			throw error("Sesión no disponible");
		}

		return authentication.getName();
	}

	private LocalDateTime ahora() {
		return LocalDateTime.ofInstant(Instant.now().truncatedTo(ChronoUnit.SECONDS), ZoneOffset.UTC);
	}

	private Instant instante(LocalDateTime fecha) {
		return fecha == null ? null : fecha.toInstant(ZoneOffset.UTC);
	}

	private BigDecimal dinero(BigDecimal importe) {
		if (importe == null || importe.signum() < 0 || importe.compareTo(new BigDecimal("99999999999999.99")) > 0) {
			throw error("Importe no válido");
		}

		return importe.setScale(2, RoundingMode.HALF_UP);
	}

	private void bloqueo(String empresa) {
		CajaControl control = em.find(CajaControl.class, empresa, LockModeType.PESSIMISTIC_WRITE);

		if (control == null) {
			throw error("Ejecuta el SQL de caja antes de continuar");
		}
	}

	private CajaSesion abierta(String empresa) {
		List<CajaSesion> cajas = em.createQuery("""
				select c
				from CajaSesion c
				where c.empresa = :e
				  and c.cierre is null
				""", CajaSesion.class).setParameter("e", empresa).setLockMode(LockModeType.PESSIMISTIC_WRITE)
				.getResultList();

		return cajas.isEmpty() ? null : cajas.get(0);
	}

	private CajaSesion cargar(Long id, String empresa) {
		CajaSesion caja = em.find(CajaSesion.class, id, LockModeType.PESSIMISTIC_WRITE);

		if (caja == null || !empresa.equals(caja.empresa)) {
			throw error("Caja no encontrada en la empresa activa");
		}

		return caja;
	}

	private List<CajaMovimiento> movimientos(Long cajaId) {
		return em.createQuery("""
				select m
				from CajaMovimiento m
				where m.cajaId = :id
				order by m.fecha, m.id
				""", CajaMovimiento.class).setParameter("id", cajaId).setLockMode(LockModeType.PESSIMISTIC_READ)
				.getResultList();
	}

	private String metodo(String valor) {
		String metodo = valor == null ? "" : valor.trim().toUpperCase(Locale.ROOT);

		return switch (metodo) {
		case "METALICO", "METÁLICO", "EFECTIVO" -> "EFECTIVO";
		case "TPV", "TARJETA" -> "TPV";
		case "TRANSFERENCIA" -> "TRANSFERENCIA";
		default -> metodo.isBlank() ? "SIN_METODO" : metodo;
		};
	}

	private BigDecimal suma(List<CajaMovimiento> movimientos, String grupo) {
		return movimientos.stream()
				.filter(movimiento -> grupo.equals("OTROS")
						? !Set.of("EFECTIVO", "TPV", "TRANSFERENCIA").contains(movimiento.metodo)
						: grupo.equals(movimiento.metodo))
				.map(movimiento -> movimiento.importe).reduce(ZERO, BigDecimal::add);
	}

	private Resumen resumen(CajaSesion caja) {
		BigDecimal efectivo;
		BigDecimal tpv;
		BigDecimal transferencia;
		BigDecimal otros;

		if (caja.cierre != null) {
			efectivo = caja.totalEfectivo;
			tpv = caja.totalTpv;
			transferencia = caja.totalTransferencia;
			otros = caja.totalOtros;
		} else {
			List<CajaMovimiento> movimientos = movimientos(caja.id);

			efectivo = suma(movimientos, "EFECTIVO");
			tpv = suma(movimientos, "TPV");
			transferencia = suma(movimientos, "TRANSFERENCIA");
			otros = suma(movimientos, "OTROS");
		}

		BigDecimal esperado = caja.saldoInicial.add(efectivo);

		BigDecimal total = efectivo.add(tpv).add(transferencia).add(otros);

		BigDecimal diferencia = caja.efectivoContado == null ? null : caja.efectivoContado.subtract(esperado);

		return new Resumen(caja.id, caja.empresa, instante(caja.apertura), instante(caja.cierre), caja.abiertaPor,
				caja.cerradaPor, caja.saldoInicial, efectivo, tpv, transferencia, otros, total, esperado,
				caja.efectivoContado, diferencia, caja.observaciones);
	}

	private String conceptoMovimiento(CajaMovimiento movimiento, String empresaCaja) {

		boolean cobroCliente = "PAGO_CLIENTE".equals(movimiento.origen);

		boolean anulacionCliente = "ANULA_PAGO_CLIENTE".equals(movimiento.origen);

		if ((!cobroCliente && !anulacionCliente) || movimiento.origenId == null) {
			return movimiento.concepto;
		}

		PagoCliente pago = em.find(PagoCliente.class, movimiento.origenId);

		if (pago == null || !empresaCaja.equals(pago.getEmpresa())) {
			return movimiento.concepto;
		}

		String referencia = null;

		if (pago.getFacturaV2Id() != null) {
			FacturaV2 factura = em.find(FacturaV2.class, pago.getFacturaV2Id());

			if (factura != null && empresaCaja.equals(factura.getEmpresa())) {
				referencia = "factura " + factura.getSerie() + "-" + factura.getNumero();
			}
		}

		/*
		 * Para pagos anteriores que guardaron la factura en observaciones pero no
		 * tienen el vínculo.
		 */
		if (referencia == null) {
			String observaciones = pago.getObservaciones();
			String prefijo = "Cobro de factura ";

			if (observaciones != null && observaciones.trim().startsWith(prefijo)) {

				String numero = observaciones.trim().substring(prefijo.length()).trim();

				if (!numero.isBlank()) {
					referencia = "factura " + numero;
				}
			}
		}

		if (referencia == null) {
			return movimiento.concepto;
		}

		String cliente = pago.getCliente() == null ? "" : pago.getCliente().getNombreApellidos();

		String concepto = (anulacionCliente ? "Anulación de cobro de " : "Cobro de ") + referencia;

		if (cliente != null && !cliente.isBlank()) {
			concepto += " · " + cliente;
		}

		concepto += " · Pago #" + pago.getId();

		if (anulacionCliente) {
			concepto += " · " + movimiento.concepto;
		}

		return concepto;
	}

	private Detalle detalle(CajaSesion caja) {
		List<Movimiento> lista = movimientos(caja.id).stream()
				.map(movimiento -> new Movimiento(movimiento.id, instante(movimiento.fecha), movimiento.metodo,
						movimiento.importe, conceptoMovimiento(movimiento, caja.empresa), movimiento.actor,
						movimiento.origen, movimiento.origenId))
				.toList();

		return new Detalle(resumen(caja), lista);
	}

	public Resumen actual() {
		CajaSesion caja = abierta(empresa());

		return caja == null ? null : resumen(caja);
	}

	public Detalle abrir() {
		String empresa = empresa();

		bloqueo(empresa);

		if (abierta(empresa) != null) {
			throw error("Ya hay una caja abierta");
		}

		CajaSesion caja = new CajaSesion();
		caja.empresa = empresa;
		caja.apertura = ahora();
		caja.abiertaPor = actor();
		caja.saldoInicial = ZERO;

		em.persist(caja);
		em.flush();

		return detalle(caja);
	}

	public Detalle cerrar(Long id, BigDecimal efectivoContado, String notas) {
		String empresa = empresa();

		bloqueo(empresa);

		CajaSesion caja = cargar(id, empresa);

		// Reintentar un cierre devuelve el cierre ya guardado.
		if (caja.cierre != null) {
			return detalle(caja);
		}

		BigDecimal contado = dinero(efectivoContado);

		String observaciones = notas == null ? "" : notas.trim();

		if (observaciones.length() > 500) {
			throw error("Observaciones demasiado largas");
		}

		List<CajaMovimiento> movimientos = movimientos(id);

		caja.totalEfectivo = suma(movimientos, "EFECTIVO");
		caja.totalTpv = suma(movimientos, "TPV");
		caja.totalTransferencia = suma(movimientos, "TRANSFERENCIA");
		caja.totalOtros = suma(movimientos, "OTROS");

		caja.efectivoContado = contado;
		caja.observaciones = observaciones;
		caja.cierre = ahora();
		caja.cerradaPor = actor();

		em.flush();

		return detalle(caja);
	}

	public Detalle ver(Long id) {
		return detalle(cargar(id, empresa()));
	}

	public Historial historial(LocalDate desde, LocalDate hasta, int pagina) {
		if (desde == null || hasta == null || hasta.isBefore(desde) || ChronoUnit.DAYS.between(desde, hasta) > 366
				|| pagina < 0 || pagina > 100000) {
			throw error("Intervalo o página no válidos");
		}

		String empresa = empresa();
		ZoneId zona = ZoneId.of("Europe/Madrid");

		LocalDateTime inicio = LocalDateTime.ofInstant(desde.atStartOfDay(zona).toInstant(), ZoneOffset.UTC);

		LocalDateTime fin = LocalDateTime.ofInstant(hasta.plusDays(1).atStartOfDay(zona).toInstant(), ZoneOffset.UTC);

		List<Resumen> cajas = em.createQuery("""
				select c
				from CajaSesion c
				where c.empresa = :e
				  and c.apertura >= :inicio
				  and c.apertura < :fin
				order by c.id desc
				""", CajaSesion.class).setParameter("e", empresa).setParameter("inicio", inicio)
				.setParameter("fin", fin).setFirstResult(pagina * 30).setMaxResults(30).getResultList().stream()
				.map(this::resumen).toList();

		long total = em.createQuery("""
				select count(c)
				from CajaSesion c
				where c.empresa = :e
				  and c.apertura >= :inicio
				  and c.apertura < :fin
				""", Long.class).setParameter("e", empresa).setParameter("inicio", inicio).setParameter("fin", fin)
				.getSingleResult();

		return new Historial(cajas, total, pagina);
	}

	public void registrarCobro(String origen, Long origenId, String empresaPago, Double importe, String forma,
			String concepto) {
		String empresa = empresa();

		if (!empresa.equals(empresaPago)) {
			throw error("El cobro no pertenece a la empresa activa");
		}

		bloqueo(empresa);

		List<CajaMovimiento> existentes = em.createQuery("""
				select m
				from CajaMovimiento m
				where m.origen = :o
				  and m.origenId = :id
				""", CajaMovimiento.class).setParameter("o", origen).setParameter("id", origenId).getResultList();

		if (!existentes.isEmpty())
			return;

		CajaSesion caja = abierta(empresa);

		// El pago se guarda igualmente, pero fuera de caja.
		if (caja == null)
			return;

		if (importe == null || !Double.isFinite(importe) || importe <= 0) {
			throw error("Cobro no válido");
		}

		insertar(caja, origen, origenId, dinero(BigDecimal.valueOf(importe)), metodo(forma), concepto);
	}

	public void anularCobro(String origen, Long origenId) {
		List<CajaMovimiento> movimientos = em.createQuery("""
				select m
				from CajaMovimiento m
				where m.origen = :o
				  and m.origenId = :id
				""", CajaMovimiento.class).setParameter("o", origen).setParameter("id", origenId).getResultList();

		if (movimientos.isEmpty())
			return;

		String empresa = empresa();

		bloqueo(empresa);

		CajaMovimiento original = movimientos.get(0);

		cargar(original.cajaId, empresa);

		String origenAnulacion = "ANULA_" + origen;

		List<CajaMovimiento> anulaciones = em.createQuery("""
				select m
				from CajaMovimiento m
				where m.origen = :o
				  and m.origenId = :id
				""", CajaMovimiento.class).setParameter("o", origenAnulacion).setParameter("id", origenId)
				.getResultList();

		if (!anulaciones.isEmpty())
			return;

		CajaSesion cajaActual = abierta(empresa);

		if (cajaActual == null) {
			throw error("Abre una caja para registrar la anulación del cobro");
		}

		insertar(cajaActual, origenAnulacion, origenId, original.importe.negate(), original.metodo,
				"Anulación del cobro #" + origenId + " de la caja #" + original.cajaId);
	}

	private void insertar(CajaSesion caja, String origen, Long origenId, BigDecimal importe, String metodo,
			String concepto) {
		CajaMovimiento movimiento = new CajaMovimiento();
		movimiento.cajaId = caja.id;
		movimiento.fecha = ahora();
		movimiento.actor = actor();
		movimiento.origen = origen;
		movimiento.origenId = origenId;
		movimiento.importe = importe;
		movimiento.metodo = metodo;

		String texto = concepto == null ? "Cobro" : concepto.trim();

		movimiento.concepto = texto.length() > 1000 ? texto.substring(0, 1000) : texto;

		em.persist(movimiento);
		em.flush();
	}

	public void bloquearTickets() {
		bloqueo(empresa());
	}

	public void registrarTicket(Long ticketId, BigDecimal total, String formaPago, String numero) {

		String empresa = empresa();

		bloqueo(empresa);

		List<CajaMovimiento> existentes = em.createQuery("""
				select m
				from CajaMovimiento m, CajaSesion c
				where m.cajaId = c.id
				  and c.empresa = :empresa
				  and m.origen = 'TICKET'
				  and m.origenId = :id
				""", CajaMovimiento.class).setParameter("empresa", empresa).setParameter("id", ticketId)
				.getResultList();

		if (!existentes.isEmpty()) {
			return;
		}

		CajaSesion caja = abierta(empresa);

		if (caja == null) {
			throw error("Abre una caja antes de cobrar el ticket");
		}

		BigDecimal importe = dinero(total);

		if (importe.signum() <= 0) {
			throw error("El total del ticket debe ser mayor que cero");
		}

		if (!Set.of("EFECTIVO", "TPV", "TRANSFERENCIA").contains(formaPago == null ? "" : formaPago)) {
			throw error("Forma de pago no válida");
		}

		insertar(caja, "TICKET", ticketId, importe, formaPago, "Venta rápida · " + numero);
	}
}
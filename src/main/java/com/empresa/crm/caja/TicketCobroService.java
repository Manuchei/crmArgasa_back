package com.empresa.crm.caja;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.empresa.crm.repositories.ProductoRepository;
import com.empresa.crm.services.TicketNumeracionService;
import com.empresa.crm.tenant.TenantContext;

@Service
public class TicketCobroService {

	@PersistenceContext
	private EntityManager em;

	private final CajaService caja;
	private final TicketNumeracionService numeracion;
	private final ObjectMapper mapper;
	private final ProductoRepository productos;

	public TicketCobroService(CajaService caja, TicketNumeracionService numeracion, ObjectMapper mapper,
			ProductoRepository productos) {

		this.caja = caja;
		this.numeracion = numeracion;
		this.mapper = mapper;
		this.productos = productos;
	}

	public record Linea(Long productoId, String articulo, Integer cantidad, BigDecimal precio, BigDecimal iva) {
	}

	public record Solicitud(UUID solicitud, String cliente, String fecha, String formaPago, BigDecimal entregado,
			List<Linea> lineas) {
	}

	public record Resultado(String numero, BigDecimal total) {
	}

	private ResponseStatusException error(String mensaje) {
		return new ResponseStatusException(HttpStatus.CONFLICT, mensaje);
	}

	@Transactional
	public Resultado cobrar(Solicitud request) {
		String empresa = TenantContext.get();

		if (!Set.of("ARGASA", "ELECTROLUGA").contains(empresa == null ? "" : empresa)) {
			throw error("Empresa no válida");
		}

		BigDecimal total = validar(request);
		String contenido = serializar(request);

		// Mismo bloqueo que apertura y cierre de caja.
		caja.bloquearTickets();

		List<?> existentes = em.createNativeQuery("""
				SELECT numero, total, contenido
				FROM ticket_venta
				WHERE empresa = :empresa
				  AND solicitud = :solicitud
				""").setParameter("empresa", empresa).setParameter("solicitud", request.solicitud().toString())
				.getResultList();

		/*
		 * Un reintento devuelve el ticket existente: no vuelve a descontar stock ni
		 * registrar el cobro.
		 */
		if (!existentes.isEmpty()) {
			Object[] fila = (Object[]) existentes.get(0);

			if (!contenido.equals(fila[2].toString())) {
				throw error("Esta venta ya se cobró con otros datos. " + "No se puede modificar el ticket cobrado.");
			}

			return new Resultado(fila[0].toString(), new BigDecimal(fila[1].toString()));
		}

		// Solo se descuenta en una venta nueva.
		descontarStock(request, empresa);

		String numero = numeracion.reservar(empresa, request.solicitud());

		em.createNativeQuery("""
				INSERT INTO ticket_venta
				    (empresa, solicitud, numero, total,
				     forma_pago, contenido)
				VALUES
				    (:empresa, :solicitud, :numero, :total,
				     :forma, :contenido)
				""").setParameter("empresa", empresa).setParameter("solicitud", request.solicitud().toString())
				.setParameter("numero", numero).setParameter("total", total).setParameter("forma", request.formaPago())
				.setParameter("contenido", contenido).executeUpdate();

		Number id = (Number) em.createNativeQuery("""
				SELECT id
				FROM ticket_venta
				WHERE empresa = :empresa
				  AND solicitud = :solicitud
				""").setParameter("empresa", empresa).setParameter("solicitud", request.solicitud().toString())
				.getSingleResult();

		caja.registrarTicket(id.longValue(), total, request.formaPago(), numero);

		return new Resultado(numero, total);
	}

	private BigDecimal validar(Solicitud request) {
		if (request == null || request.solicitud() == null) {
			throw error("Falta la solicitud del ticket");
		}

		if (request.cliente() == null || request.cliente().isBlank() || request.cliente().length() > 250) {
			throw error("Cliente no válido");
		}

		try {
			LocalDateTime.parse(request.fecha() == null ? "" : request.fecha());
		} catch (DateTimeParseException exception) {
			throw error("Fecha no válida");
		}

		if (!Set.of("EFECTIVO", "TPV", "TRANSFERENCIA")
				.contains(request.formaPago() == null ? "" : request.formaPago())) {
			throw error("Forma de pago no válida");
		}

		if (request.lineas() == null || request.lineas().isEmpty() || request.lineas().size() > 500) {
			throw error("Añade entre 1 y 500 artículos");
		}

		BigDecimal total = new BigDecimal("0.00");

		for (Linea linea : request.lineas()) {
			if (linea == null || linea.articulo() == null || linea.articulo().isBlank()
					|| linea.articulo().length() > 1000 || linea.cantidad() == null || linea.cantidad() <= 0
					|| linea.cantidad() > 1000000 || linea.precio() == null || linea.precio().signum() < 0
					|| linea.precio().stripTrailingZeros().scale() > 2
					|| linea.precio().compareTo(new BigDecimal("999999999.99")) > 0 || linea.iva() == null
					|| linea.iva().signum() < 0 || linea.iva().compareTo(new BigDecimal("100")) > 0) {

				throw error("Revisa los artículos, cantidades, precios e IVA");
			}

			if (linea.productoId() != null && linea.productoId() <= 0) {
				throw error("Producto no válido");
			}

			BigDecimal importe = linea.precio().multiply(BigDecimal.valueOf(linea.cantidad())).setScale(2,
					RoundingMode.HALF_UP);

			total = total.add(importe);
		}

		if (total.signum() <= 0 || total.compareTo(new BigDecimal("99999999999999.99")) > 0) {
			throw error("Total del ticket no válido");
		}

		if ("EFECTIVO".equals(request.formaPago())
				&& (request.entregado() == null || request.entregado().compareTo(total) < 0)) {
			throw error("El efectivo entregado debe cubrir el total");
		}

		return total;
	}

	private String serializar(Solicitud request) {
		try {
			return mapper.writeValueAsString(request);
		} catch (JsonProcessingException exception) {
			throw error("No se ha podido guardar el ticket");
		}
	}

	private void descontarStock(Solicitud request, String empresa) {

		/*
		 * Agrupa las cantidades del mismo producto. Ordena por ID para actualizar
		 * siempre en el mismo orden.
		 */
		Map<Long, Integer> cantidades = new TreeMap<>();

		for (Linea linea : request.lineas()) {
			// Un artículo manual no pertenece al inventario.
			if (linea.productoId() == null) {
				continue;
			}

			cantidades.merge(linea.productoId(), linea.cantidad(), Math::addExact);
		}

		for (Map.Entry<Long, Integer> entrada : cantidades.entrySet()) {

			int actualizados = productos.decrementUnidadesIfAvailable(entrada.getKey(), entrada.getValue(), empresa);

			if (actualizados != 1) {
				throw error("Stock insuficiente o producto no disponible " + "en esta empresa. Producto #"
						+ entrada.getKey() + ". Actualiza los productos y revisa el ticket.");
			}
		}
	}
}
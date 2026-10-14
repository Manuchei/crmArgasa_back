package com.empresa.crm.controllers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.empresa.crm.entities.ArticuloObra;
import com.empresa.crm.entities.PagoObra;
import com.empresa.crm.entities.Proveedor;
import com.empresa.crm.entities.Trabajo;
import com.empresa.crm.services.TrabajoService;
import com.empresa.crm.tenant.TenantContext;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

@RestController
@RequestMapping("/api/obras-proveedor")
@CrossOrigin(origins = "http://localhost:4200")
@Transactional
public class ObraProveedorController {

	@PersistenceContext
	private EntityManager em;

	private final TrabajoService trabajoService;

	public ObraProveedorController(TrabajoService trabajoService) {
		this.trabajoService = trabajoService;
	}

	public record CrearObraRequest(String descripcion, LocalDate fecha, String nombreContacto, String direccionObra,
			String telefonoObra) {
	}

	public record ArticuloRequest(LocalDate fecha, String descripcion, BigDecimal cantidad, BigDecimal precioUnitario) {
	}

	public record PagoRequest(LocalDate fecha, BigDecimal importe, String concepto) {
	}

	public record EstadoRequest(String estado) {
	}

	@PostMapping("/proveedor/{proveedorId}")
	public Trabajo crear(@PathVariable Long proveedorId, @RequestBody CrearObraRequest req) {

		String empresa = empresaActual();
		Proveedor proveedor = em.find(Proveedor.class, proveedorId);

		if (proveedor == null || !empresa.equals(proveedor.getEmpresa())) {
			throw error(HttpStatus.NOT_FOUND, "Proveedor no encontrado");
		}

		if (req == null || req.fecha() == null) {
			throw error(HttpStatus.BAD_REQUEST, "La fecha es obligatoria");
		}

		Trabajo t = new Trabajo();
		t.setDescripcion(texto(req.descripcion(), 500, "Nombre de la obra"));
		t.setNombreContacto(texto(req.nombreContacto(), 150, "Nombre del contacto"));
		t.setDireccionObra(texto(req.direccionObra(), 500, "Dirección"));
		t.setTelefonoObra(texto(req.telefonoObra(), 50, "Teléfono"));

		t.setFecha(req.fecha());
		t.setProveedor(proveedor);
		t.setEmpresa(empresa);
		t.setObra(true);
		t.setEstadoObra("PENDIENTE");
		t.setUnidades(1);
		t.setPrecioUnitario(0.0);
		t.setImporte(0.0);
		t.setImportePagado(0.0);
		t.setDescuento(0.0);

		return trabajoService.save(t);
	}

	@GetMapping("/{id}")
	public Map<String, Object> detalle(@PathVariable Long id) {
		Trabajo t = cargar(id);
		return respuesta(t);
	}

	@PostMapping("/{id}/articulos")
	public Map<String, Object> agregarArticulo(@PathVariable Long id, @RequestBody ArticuloRequest req) {

		Trabajo t = cargar(id);
		comprobarEditable(t);

		if (req == null || req.fecha() == null) {
			throw error(HttpStatus.BAD_REQUEST, "La fecha es obligatoria");
		}

		if (req.cantidad() == null || req.cantidad().signum() <= 0
				|| req.cantidad().compareTo(new BigDecimal("999999999.999")) > 0) {
			throw error(HttpStatus.BAD_REQUEST, "Cantidad no válida");
		}

		BigDecimal cantidad = req.cantidad().setScale(3, RoundingMode.HALF_UP);

		if (cantidad.signum() <= 0) {
			throw error(HttpStatus.BAD_REQUEST, "Cantidad demasiado pequeña");
		}

		BigDecimal precio = dinero(req.precioUnitario(), false);

		ArticuloObra a = new ArticuloObra();
		a.setTrabajo(t);
		a.setFecha(req.fecha());
		a.setDescripcion(texto(req.descripcion(), 500, "Artículo"));
		a.setCantidad(cantidad);
		a.setPrecioUnitario(precio);
		a.setImporte(cantidad.multiply(precio).setScale(2, RoundingMode.HALF_UP));

		em.persist(a);
		em.flush();
		recalcular(t);

		return respuesta(t);
	}

	@PostMapping("/{id}/pagos")
	public Map<String, Object> agregarPago(@PathVariable Long id, @RequestBody PagoRequest req) {

		Trabajo t = cargar(id);

		if ("CANCELADA".equals(t.getEstadoObra())) {
			throw error(HttpStatus.CONFLICT, "No se pueden añadir pagos a una obra cancelada");
		}

		if (req == null || req.fecha() == null) {
			throw error(HttpStatus.BAD_REQUEST, "La fecha es obligatoria");
		}

		String concepto = req.concepto() == null ? "" : req.concepto().trim();

		if (concepto.length() > 500) {
			throw error(HttpStatus.BAD_REQUEST, "Concepto demasiado largo");
		}

		PagoObra p = new PagoObra();
		p.setTrabajo(t);
		p.setFecha(req.fecha());
		p.setImporte(dinero(req.importe(), true));
		p.setConcepto(concepto);

		em.persist(p);
		em.flush();
		recalcular(t);

		return respuesta(t);
	}

	@PatchMapping("/{id}/estado")
	public Map<String, Object> cambiarEstado(@PathVariable Long id, @RequestBody EstadoRequest req) {

		Trabajo t = cargar(id);

		if (req == null || req.estado() == null
				|| !Set.of("PENDIENTE", "EN_CURSO", "FINALIZADA", "CANCELADA").contains(req.estado())) {
			throw error(HttpStatus.BAD_REQUEST, "Estado no válido");
		}

		t.setEstadoObra(req.estado());
		em.flush();

		return respuesta(t);
	}

	private Trabajo cargar(Long id) {
		String empresa = empresaActual();

		List<Trabajo> resultado = em.createQuery("""
				select t from Trabajo t
				where t.id = :id and t.empresa = :empresa
				  and t.obra = true and t.proveedor is not null
				""", Trabajo.class).setParameter("id", id).setParameter("empresa", empresa)
				.setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();

		if (resultado.isEmpty()) {
			throw error(HttpStatus.NOT_FOUND, "Obra no encontrada");
		}

		return resultado.get(0);
	}

	private void comprobarEditable(Trabajo t) {
		if ("FINALIZADA".equals(t.getEstadoObra()) || "CANCELADA".equals(t.getEstadoObra())) {
			throw error(HttpStatus.CONFLICT, "Reabre la obra antes de añadir artículos");
		}

		if (t.getFactura() != null || t.isEntregado()) {
			throw error(HttpStatus.CONFLICT, "El trabajo ya está facturado o entregado");
		}
	}

	private List<ArticuloObra> articulos(Trabajo t) {
		return em.createQuery("""
				select a from ArticuloObra a
				where a.trabajo.id = :id
				order by a.fecha, a.id
				""", ArticuloObra.class).setParameter("id", t.getId()).getResultList();
	}

	private List<PagoObra> pagos(Trabajo t) {
		return em.createQuery("""
				select p from PagoObra p
				where p.trabajo.id = :id
				order by p.fecha, p.id
				""", PagoObra.class).setParameter("id", t.getId()).getResultList();
	}

	private void recalcular(Trabajo t) {
		BigDecimal total = articulos(t).stream().map(ArticuloObra::getImporte).reduce(BigDecimal.ZERO, BigDecimal::add);

		BigDecimal pagado = pagos(t).stream().map(PagoObra::getImporte).reduce(BigDecimal.ZERO, BigDecimal::add);

		// El callback de Trabajo calcula importe desde precioUnitario.
		t.setUnidades(1);
		t.setDescuento(0.0);
		t.setPrecioUnitario(total.doubleValue());
		t.setImporte(total.doubleValue());
		t.setImportePagado(pagado.doubleValue());
		em.flush();
	}

	private Map<String, Object> respuesta(Trabajo t) {
		BigDecimal total = BigDecimal.valueOf(t.getImporte());
		BigDecimal pagado = BigDecimal.valueOf(t.getImportePagado());

		return Map.of("trabajo", t, "articulos", articulos(t), "pagos", pagos(t), "pendiente",
				total.subtract(pagado).max(BigDecimal.ZERO), "anticipo", pagado.subtract(total).max(BigDecimal.ZERO));
	}

	private String empresaActual() {
		String empresa = TenantContext.get();

		if (empresa == null || empresa.isBlank()) {
			throw error(HttpStatus.BAD_REQUEST, "Empresa no seleccionada");
		}

		return empresa;
	}

	private String texto(String valor, int limite, String campo) {
		if (valor == null || valor.isBlank()) {
			throw error(HttpStatus.BAD_REQUEST, campo + " es obligatorio");
		}

		String limpio = valor.trim();

		if (limpio.length() > limite) {
			throw error(HttpStatus.BAD_REQUEST, campo + " demasiado largo");
		}

		return limpio;
	}

	private BigDecimal dinero(BigDecimal valor, boolean positivo) {
		if (valor == null || valor.signum() < 0 || valor.compareTo(new BigDecimal("999999999999.99")) > 0) {
			throw error(HttpStatus.BAD_REQUEST, "Importe no válido");
		}

		BigDecimal resultado = valor.setScale(2, RoundingMode.HALF_UP);

		if (positivo && resultado.signum() <= 0) {
			throw error(HttpStatus.BAD_REQUEST, "El pago debe ser mayor que cero");
		}

		return resultado;
	}

	private ResponseStatusException error(HttpStatus estado, String mensaje) {
		return new ResponseStatusException(estado, mensaje);
	}
}
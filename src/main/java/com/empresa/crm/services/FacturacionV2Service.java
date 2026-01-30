package com.empresa.crm.services;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.empresa.crm.dto.facturacionv2.CrearFacturaV2Request;
import com.empresa.crm.dto.facturacionv2.FacturaV2Response;
import com.empresa.crm.dto.facturacionv2.LineaFacturaV2Response;
import com.empresa.crm.entities.LineaAlbaranCliente;
import com.empresa.crm.entities.ServicioCliente;
import com.empresa.crm.entities.facturacionV2.ContadorFacturaV2;
import com.empresa.crm.entities.facturacionV2.FacturaV2;
import com.empresa.crm.entities.facturacionV2.LineaFacturaV2;
import com.empresa.crm.repositories.ClienteRepository;
import com.empresa.crm.repositories.LineaAlbaranClienteRepository;
import com.empresa.crm.repositories.ServicioClienteRepository;
import com.empresa.crm.repositories.facturacionV2.ContadorFacturaV2Repository;
import com.empresa.crm.repositories.facturacionV2.FacturaV2Repository;
import com.empresa.crm.tenant.TenantContext;

import com.empresa.crm.entities.facturacionV2.FacturaV2;
import com.empresa.crm.entities.facturacionV2.LineaFacturaV2;
import com.empresa.crm.dto.facturacionv2.FacturaV2Response;
import com.empresa.crm.dto.facturacionv2.LineaFacturaV2Response;

import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class FacturacionV2Service {

	private final ClienteRepository clienteRepo;
	private final ServicioClienteRepository servicioRepo;
	private final LineaAlbaranClienteRepository lineaRepo;
	private final FacturaV2Repository facturaRepo;
	private final ContadorFacturaV2Repository contadorRepo;

	public FacturacionV2Service(ClienteRepository clienteRepo, ServicioClienteRepository servicioRepo,
			LineaAlbaranClienteRepository lineaRepo, FacturaV2Repository facturaRepo,
			ContadorFacturaV2Repository contadorRepo) {
		this.clienteRepo = clienteRepo;
		this.servicioRepo = servicioRepo;
		this.lineaRepo = lineaRepo;
		this.facturaRepo = facturaRepo;
		this.contadorRepo = contadorRepo;
	}
	
	@Transactional(readOnly = true)
	public FacturaV2Response getFacturaById(Long id) {

	    String empresa = TenantContext.get();
	    if (empresa == null || empresa.isBlank()) {
	        throw new RuntimeException("Empresa no seleccionada");
	    }

	    FacturaV2 factura = facturaRepo.findByIdAndEmpresa(id, empresa)
	            .orElseThrow(() -> new RuntimeException("Factura no encontrada"));

	    return toFacturaV2Response(factura);
	}

	
	private FacturaV2Response toFacturaV2Response(FacturaV2 f) {

	    List<LineaFacturaV2Response> lineas = f.getLineas().stream()
	            .map(this::toLineaFacturaV2Response)
	            .toList();

	    // ✅ Si FacturaV2Response es RECORD:
	    return new FacturaV2Response(
	            f.getId(),
	            f.getEmpresa(),
	            f.getSerie(),
	            f.getNumero(),
	            f.getFechaEmision(),
	            f.getEstado(),
	            f.getBaseImponible(),
	            f.getIvaTotal(),
	            f.getTotal(),
	            lineas
	    );
	}

	private LineaFacturaV2Response toLineaFacturaV2Response(LineaFacturaV2 l) {

	    // ✅ Si LineaFacturaV2Response es RECORD:
	    return new LineaFacturaV2Response(
	            l.getId(),
	            l.getTipoOrigen(),
	            l.getOrigenId(),
	            l.getDescripcion(),
	            l.getCantidad(),
	            l.getPrecioUnitario(),
	            l.getSubtotal(),
	            l.getIvaPct(),
	            l.getTotalLinea()
	    );
	}



	
	// ✅ LISTADO: devuelve BORRADOR + EMITIDA + etc (filtrable)
	@Transactional(readOnly = true)
	public List<FacturaV2Response> listarFacturas(Long clienteId, String estado) {
		String empresa = TenantContext.get();
		if (empresa == null || empresa.isBlank())
			throw new RuntimeException("Empresa no seleccionada");

		String est = (estado == null || estado.isBlank()) ? null : estado.trim();

		List<FacturaV2> list;

		if (clienteId != null && est != null) {
			list = facturaRepo.findByEmpresaAndCliente_IdAndEstadoOrderByFechaEmisionDesc(empresa, clienteId, est);
		} else if (clienteId != null) {
			list = facturaRepo.findByEmpresaAndCliente_IdOrderByFechaEmisionDesc(empresa, clienteId);
		} else if (est != null) {
			list = facturaRepo.findByEmpresaAndEstadoOrderByFechaEmisionDesc(empresa, est);
		} else {
			list = facturaRepo.findByEmpresaOrderByFechaEmisionDesc(empresa);
		}

		return list.stream().map(this::mapResponse).toList();
	}

	@Transactional
	public FacturaV2Response crearBorrador(CrearFacturaV2Request req) {
		String empresa = TenantContext.get();
		if (empresa == null || empresa.isBlank())
			throw new RuntimeException("Empresa no seleccionada");

		Long clienteId = req.clienteId();
		String serie = (req.serie() == null || req.serie().isBlank()) ? "A" : req.serie().trim();

		// 1) validar cliente pertenece a empresa
		var cliente = clienteRepo.findByIdAndEmpresa(clienteId, empresa)
				.orElseThrow(() -> new RuntimeException("Cliente no existe o no pertenece a la empresa"));

		List<Long> servicioIds = (req.servicioId() == null) ? List.of() : req.servicioId();
		List<Long> lineaIds = (req.lineasAlbaranIds() == null) ? List.of() : req.lineasAlbaranIds();

		if (servicioIds.isEmpty() && lineaIds.isEmpty()) {
			throw new RuntimeException("Debes seleccionar al menos un servicio o una línea de albarán");
		}

		// 2) cargar selección VALIDANDO “pendiente”
		List<ServicioCliente> servicios = servicioIds.isEmpty() ? List.of()
				: servicioRepo.findByEmpresaAndIdInAndFacturaV2IdIsNull(empresa, servicioIds);

		if (servicios.size() != servicioIds.size()) {
			throw new RuntimeException(
					"Hay servicios que no existen, no son de la empresa o ya están reservados/facturados");
		}

		boolean serviciosOtroCliente = servicios.stream().anyMatch(s -> !s.getCliente().getId().equals(clienteId));
		if (serviciosOtroCliente)
			throw new RuntimeException("Hay servicios que no pertenecen al cliente");

		List<LineaAlbaranCliente> lineas = lineaIds.isEmpty() ? List.of()
				: lineaRepo.findPendientesSeleccionadas(lineaIds, empresa, clienteId);

		if (lineas.size() != lineaIds.size()) {
			throw new RuntimeException(
					"Hay líneas no válidas (no pendientes, otro cliente, otra empresa o albarán no confirmado)");
		}

		// 3) crear factura con número correlativo por empresa+serie
		int numero = siguienteNumero(empresa, serie);

		FacturaV2 factura = new FacturaV2();
		factura.setCliente(cliente); // ✅ CLAVE
		factura.setEmpresa(empresa); // ✅ CLAVE
		factura.setSerie(serie);
		factura.setNumero(numero);
		factura.setFechaEmision(LocalDate.now());
		factura.setEstado("BORRADOR");

		// 4) líneas desde servicios + albarán
		List<LineaFacturaV2> lineasFactura = new ArrayList<>();

		for (ServicioCliente s : servicios) {
			LineaFacturaV2 lf = new LineaFacturaV2();
			lf.setFactura(factura);
			lf.setTipoOrigen("SERVICIO");
			lf.setOrigenId(s.getId());
			lf.setDescripcion(s.getDescripcion());
			lf.setCantidad(1.0);
			lf.setPrecioUnitario(s.getImporte());
			lf.setSubtotal(s.getImporte());
			lf.setIvaPct(21.0);
			lf.setTotalLinea(s.getImporte() * 1.21);
			lineasFactura.add(lf);
		}

		for (LineaAlbaranCliente l : lineas) {
			double subtotal = (l.getTotalLinea() != null) ? l.getTotalLinea() : (l.getUnidades() * l.getPrecio());

			LineaFacturaV2 lf = new LineaFacturaV2();
			lf.setFactura(factura);
			lf.setTipoOrigen("ALBARAN_LINEA");
			lf.setOrigenId(l.getId());
			lf.setDescripcion(l.getDescripcion());
			lf.setCantidad(l.getUnidades());
			lf.setPrecioUnitario(l.getPrecio());
			lf.setSubtotal(subtotal);
			lf.setIvaPct(21.0);
			lf.setTotalLinea(subtotal * 1.21);
			lineasFactura.add(lf);
		}

		factura.getLineas().addAll(lineasFactura);

		// 5) recalcular totales
		recalcularTotales(factura);

		// 6) guardar factura
		FacturaV2 guardada = facturaRepo.save(factura);

		// 7) RESERVAR items
		servicios.forEach(s -> s.setFacturaV2Id(guardada.getId()));
		lineas.forEach(l -> l.setFacturaV2Id(guardada.getId()));

		return mapResponse(guardada);
	}

	@Transactional
	public void cancelarBorrador(Long facturaId) {
		String empresa = TenantContext.get();
		if (empresa == null || empresa.isBlank())
			throw new RuntimeException("Empresa no seleccionada");

		FacturaV2 factura = facturaRepo.findByIdAndEmpresa(facturaId, empresa)
				.orElseThrow(() -> new RuntimeException("Factura no existe"));

		if (!"BORRADOR".equals(factura.getEstado())) {
			throw new RuntimeException("Solo se puede cancelar una factura en BORRADOR");
		}

		List<Long> servicioIds = factura.getLineas().stream().filter(l -> "SERVICIO".equals(l.getTipoOrigen()))
				.map(LineaFacturaV2::getOrigenId).toList();

		if (!servicioIds.isEmpty()) {
			List<ServicioCliente> servicios = servicioRepo.findByEmpresaAndIdIn(empresa, servicioIds);
			servicios.forEach(s -> {
				if (facturaId.equals(s.getFacturaV2Id()))
					s.setFacturaV2Id(null);
			});
		}

		List<Long> lineaIds = factura.getLineas().stream().filter(l -> "ALBARAN_LINEA".equals(l.getTipoOrigen()))
				.map(LineaFacturaV2::getOrigenId).toList();

		if (!lineaIds.isEmpty()) {
			List<LineaAlbaranCliente> lineas = lineaRepo.findAllById(lineaIds);
			lineas.forEach(l -> {
				if (empresa.equals(l.getEmpresa()) && facturaId.equals(l.getFacturaV2Id())) {
					l.setFacturaV2Id(null);
				}
			});
		}

		facturaRepo.delete(factura);
	}

	@Transactional
	public FacturaV2Response emitir(Long facturaId) {
		String empresa = TenantContext.get();
		if (empresa == null || empresa.isBlank())
			throw new RuntimeException("Empresa no seleccionada");

		FacturaV2 factura = facturaRepo.findByIdAndEmpresa(facturaId, empresa)
				.orElseThrow(() -> new RuntimeException("Factura no existe"));

		if (!"BORRADOR".equals(factura.getEstado())) {
			throw new RuntimeException("Solo se puede emitir una factura en BORRADOR");
		}

		validarReservasSiguenVivas(empresa, factura);

		recalcularTotales(factura);

		String hash = generarHashEmision(factura);
		factura.setHashEmision(hash);

		factura.setEstado("EMITIDA");

		FacturaV2 guardada = facturaRepo.save(factura);
		return mapResponse(guardada);
	}

	// ---------------- helpers ----------------

	private int siguienteNumero(String empresa, String serie) {
		ContadorFacturaV2 c = contadorRepo.findByEmpresaAndSerie(empresa, serie).orElseGet(() -> {
			ContadorFacturaV2 nuevo = new ContadorFacturaV2();
			nuevo.setEmpresa(empresa);
			nuevo.setSerie(serie);
			nuevo.setSiguienteNumero(1);
			return contadorRepo.save(nuevo);
		});

		int actual = c.getSiguienteNumero();
		c.setSiguienteNumero(actual + 1);
		contadorRepo.save(c);
		return actual;
	}

	private void recalcularTotales(FacturaV2 f) {
		double base = f.getLineas().stream().mapToDouble(LineaFacturaV2::getSubtotal).sum();
		double iva = f.getLineas().stream().mapToDouble(l -> l.getSubtotal() * (l.getIvaPct() / 100.0)).sum();
		f.setBaseImponible(round2(base));
		f.setIvaTotal(round2(iva));
		f.setTotal(round2(base + iva));
	}

	private double round2(double v) {
		return Math.round(v * 100.0) / 100.0;
	}

	private FacturaV2Response mapResponse(FacturaV2 f) {
		var lineas = f.getLineas().stream()
				.map(l -> new LineaFacturaV2Response(l.getId(), l.getTipoOrigen(), l.getOrigenId(), l.getDescripcion(),
						l.getCantidad(), l.getPrecioUnitario(), l.getSubtotal(), l.getIvaPct(), l.getTotalLinea()))
				.toList();

		return new FacturaV2Response(f.getId(), f.getEmpresa(), f.getSerie(), f.getNumero(), f.getFechaEmision(),
				f.getEstado(), f.getBaseImponible(), f.getIvaTotal(), f.getTotal(), lineas);
	}

	private void validarReservasSiguenVivas(String empresa, FacturaV2 factura) {
		Long facturaId = factura.getId();

		List<Long> servicioIds = factura.getLineas().stream().filter(l -> "SERVICIO".equals(l.getTipoOrigen()))
				.map(LineaFacturaV2::getOrigenId).toList();

		if (!servicioIds.isEmpty()) {
			List<ServicioCliente> servicios = servicioRepo.findByEmpresaAndIdIn(empresa, servicioIds);
			if (servicios.size() != servicioIds.size())
				throw new RuntimeException("Servicios no válidos");

			boolean algunoNoReservadoPorEsta = servicios.stream()
					.anyMatch(s -> s.getFacturaV2Id() == null || !facturaId.equals(s.getFacturaV2Id()));

			if (algunoNoReservadoPorEsta)
				throw new RuntimeException("Hay servicios no reservados por esta factura");
		}

		List<Long> lineaIds = factura.getLineas().stream().filter(l -> "ALBARAN_LINEA".equals(l.getTipoOrigen()))
				.map(LineaFacturaV2::getOrigenId).toList();

		if (!lineaIds.isEmpty()) {
			List<LineaAlbaranCliente> lineas = lineaRepo.findAllById(lineaIds);
			if (lineas.size() != lineaIds.size())
				throw new RuntimeException("Líneas de albarán no válidas");

			boolean algunaNoReservada = lineas.stream().anyMatch(l -> !empresa.equals(l.getEmpresa())
					|| l.getFacturaV2Id() == null || !facturaId.equals(l.getFacturaV2Id()));

			if (algunaNoReservada)
				throw new RuntimeException("Hay líneas no reservadas por esta factura");
		}
	}

	private String generarHashEmision(FacturaV2 f) {
		StringBuilder sb = new StringBuilder();
		sb.append(f.getEmpresa()).append("|").append(f.getSerie()).append("|").append(f.getNumero()).append("|")
				.append(f.getFechaEmision()).append("|");

		f.getLineas()
				.forEach(l -> sb.append(l.getTipoOrigen()).append(":").append(l.getOrigenId()).append(":")
						.append(l.getDescripcion()).append(":").append(l.getCantidad()).append(":")
						.append(l.getPrecioUnitario()).append(":").append(l.getIvaPct()).append("|"));

		return sha256(sb.toString());
	}

	private String sha256(String input) {
		try {
			var md = java.security.MessageDigest.getInstance("SHA-256");
			byte[] hash = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder();
			for (byte b : hash)
				hex.append(String.format("%02x", b));
			return hex.toString();
		} catch (Exception e) {
			throw new RuntimeException("Error generando hash", e);
		}
	}
}

package com.empresa.crm.services;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.empresa.crm.dto.facturacionv2.ActualizarFacturaV2Request;
import com.empresa.crm.dto.facturacionv2.ClienteDTO;
import com.empresa.crm.dto.facturacionv2.CrearFacturaV2Request;
import com.empresa.crm.dto.facturacionv2.EmpresaEmisoraDTO;
import com.empresa.crm.dto.facturacionv2.FacturaV2Response;
import com.empresa.crm.dto.facturacionv2.LineaFacturaV2Response;
import com.empresa.crm.dto.facturacionv2.LineaFacturaV2UpdateRequest;
import com.empresa.crm.entities.Cliente;
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

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

@Service
public class FacturacionV2Service {

	private final ClienteRepository clienteRepo;
	private final ServicioClienteRepository servicioRepo;
	private final LineaAlbaranClienteRepository lineaRepo;
	private final FacturaV2Repository facturaRepo;
	private final ContadorFacturaV2Repository contadorRepo;
	private final AlbaranClienteService albaranClienteService;

	@PersistenceContext
	private EntityManager entityManager;

	public FacturacionV2Service(ClienteRepository clienteRepo, ServicioClienteRepository servicioRepo,
			LineaAlbaranClienteRepository lineaRepo, FacturaV2Repository facturaRepo,
			ContadorFacturaV2Repository contadorRepo, AlbaranClienteService albaranClienteService) {

		this.clienteRepo = clienteRepo;
		this.servicioRepo = servicioRepo;
		this.lineaRepo = lineaRepo;
		this.facturaRepo = facturaRepo;
		this.contadorRepo = contadorRepo;
		this.albaranClienteService = albaranClienteService;
	}

	@Transactional(readOnly = true)
	public FacturaV2Response getFacturaById(Long id) {
		String empresa = empresaActual();

		FacturaV2 factura = facturaRepo.findByIdAndEmpresaWithClienteAndLineas(id, empresa)
				.orElseThrow(() -> new IllegalArgumentException("Factura no encontrada"));

		return mapResponse(factura);
	}

	@Transactional(readOnly = true)
	public List<FacturaV2Response> listarFacturas(Long clienteId, String estado) {

		String empresa = empresaActual();
		String est = tieneTexto(estado) ? estado.trim() : null;

		List<FacturaV2> facturas;

		if (clienteId != null && est != null) {
			facturas = facturaRepo.findByEmpresaAndCliente_IdAndEstadoOrderByFechaEmisionDesc(empresa, clienteId, est);
		} else if (clienteId != null) {
			facturas = facturaRepo.findByEmpresaAndCliente_IdOrderByFechaEmisionDesc(empresa, clienteId);
		} else if (est != null) {
			facturas = facturaRepo.findByEmpresaAndEstadoOrderByFechaEmisionDesc(empresa, est);
		} else {
			facturas = facturaRepo.findByEmpresaOrderByFechaEmisionDesc(empresa);
		}

		return facturas.stream().map(this::mapResponse).toList();
	}

	@Transactional
	public FacturaV2Response actualizarBorrador(Long facturaId, ActualizarFacturaV2Request req) {

		String empresa = empresaActual();

		FacturaV2 factura = facturaRepo.findByIdAndEmpresaWithClienteAndLineas(facturaId, empresa)
				.orElseThrow(() -> new IllegalArgumentException("Factura no existe"));

		comprobarBorradorEditable(factura);

		if (req == null) {
			throw new IllegalArgumentException("No se recibieron datos para actualizar");
		}

		if (req.lineas() == null || req.lineas().isEmpty()) {
			throw new IllegalArgumentException("La factura debe tener al menos una línea");
		}

		Map<Long, LineaFacturaV2> lineasActuales = factura.getLineas().stream()
				.collect(Collectors.toMap(LineaFacturaV2::getId, Function.identity()));

		if (req.lineas().size() != factura.getLineas().size()) {
			throw new IllegalArgumentException("No se pueden añadir ni quitar líneas en esta versión");
		}

		var idsRecibidos = new HashSet<Long>();

		for (LineaFacturaV2UpdateRequest lineaReq : req.lineas()) {
			if (lineaReq == null || lineaReq.id() == null) {
				throw new IllegalArgumentException("Todas las líneas deben incluir su id");
			}

			if (!idsRecibidos.add(lineaReq.id())) {
				throw new IllegalArgumentException("Hay líneas repetidas en la petición");
			}

			LineaFacturaV2 linea = lineasActuales.get(lineaReq.id());

			if (linea == null) {
				throw new IllegalArgumentException("Hay líneas que no pertenecen a la factura");
			}

			String descripcion = lineaReq.descripcion() == null ? "" : lineaReq.descripcion().trim();

			validarDatosLinea(descripcion, lineaReq.cantidad(), lineaReq.precioUnitario(), lineaReq.descuentoPct(),
					lineaReq.ivaPct());

			linea.setDescripcion(descripcion);
			linea.setCantidad(lineaReq.cantidad());
			linea.setPrecioUnitario(lineaReq.precioUnitario());
			linea.setDescuentoPct(lineaReq.descuentoPct());
			linea.setIvaPct(lineaReq.ivaPct());
			linea.recalcular();
		}

		if (req.fechaEmision() != null) {
			factura.setFechaEmision(req.fechaEmision());
		}

		recalcularTotales(factura);

		return mapResponse(facturaRepo.save(factura));
	}

	@Transactional
	public FacturaV2Response crearBorrador(CrearFacturaV2Request req) {
		String empresa = empresaActual();

		if (req == null || req.clienteId() == null) {
			throw new IllegalArgumentException("Cliente no indicado");
		}

		Long clienteId = req.clienteId();
		String serie = tieneTexto(req.serie()) ? req.serie().trim() : "A";

		Cliente cliente = clienteRepo.findByIdAndEmpresa(clienteId, empresa)
				.orElseThrow(() -> new IllegalArgumentException("Cliente no existe o no pertenece a la empresa"));

		List<Long> servicioIds = req.servicioId() == null ? List.of() : req.servicioId();

		List<Long> lineaIds = req.lineasAlbaranIds() == null ? List.of() : req.lineasAlbaranIds();

		if (servicioIds.isEmpty() && lineaIds.isEmpty()) {
			throw new IllegalArgumentException("Debes seleccionar al menos un servicio o una línea de albarán");
		}

		if (servicioIds.contains(null) || lineaIds.contains(null)
				|| new HashSet<>(servicioIds).size() != servicioIds.size()
				|| new HashSet<>(lineaIds).size() != lineaIds.size()) {
			throw new IllegalArgumentException("La selección contiene identificadores inválidos o repetidos");
		}

		List<ServicioCliente> servicios = servicioIds.isEmpty() ? List.of()
				: servicioRepo.findByEmpresaAndIdInAndFacturaV2IdIsNull(empresa, servicioIds);

		if (servicios.size() != servicioIds.size()) {
			throw new IllegalArgumentException(
					"Hay servicios que no existen, no son de la empresa " + "o ya están reservados/facturados");
		}

		if (servicios.stream().anyMatch(s -> s.getCliente() == null || !clienteId.equals(s.getCliente().getId()))) {
			throw new IllegalArgumentException("Hay servicios que no pertenecen al cliente");
		}

		List<LineaAlbaranCliente> lineas = lineaIds.isEmpty() ? List.of()
				: lineaRepo.findPendientesSeleccionadas(lineaIds, empresa, clienteId);

		if (lineas.size() != lineaIds.size()) {
			throw new IllegalArgumentException(
					"Hay líneas no válidas: no pendientes, otro cliente, " + "otra empresa o albarán no confirmado");
		}

		FacturaV2 factura = new FacturaV2();
		factura.setCliente(cliente);
		factura.setEmpresa(empresa);
		factura.setSerie(serie);
		factura.setNumero(siguienteNumero(empresa, serie));
		factura.setFechaEmision(LocalDate.now(ZoneId.of("Europe/Madrid")));
		factura.setEstado("BORRADOR");

		List<LineaFacturaV2> lineasFactura = new ArrayList<>();

		for (ServicioCliente servicio : servicios) {
			LineaFacturaV2 linea = new LineaFacturaV2();
			linea.setFactura(factura);
			linea.setTipoOrigen("SERVICIO");
			linea.setOrigenId(servicio.getId());
			linea.setDescripcion(servicio.getDescripcion());
			linea.setCantidad(1.0);
			linea.setPrecioUnitario(servicio.getImporte());
			linea.setDescuentoPct(0.0);
			linea.setIvaPct(21.0);

			validarLinea(linea);
			linea.recalcular();
			lineasFactura.add(linea);
		}

		for (LineaAlbaranCliente origen : lineas) {
			LineaFacturaV2 linea = new LineaFacturaV2();
			linea.setFactura(factura);
			linea.setTipoOrigen("ALBARAN_LINEA");
			linea.setOrigenId(origen.getId());
			linea.setDescripcion(origen.getDescripcion());
			linea.setCantidad(origen.getUnidades());
			linea.setPrecioUnitario(origen.getPrecio());
			linea.setDescuentoPct(origen.getDtoPct() == null ? 0.0 : origen.getDtoPct());
			linea.setIvaPct(21.0);

			validarLinea(linea);
			linea.recalcular();
			lineasFactura.add(linea);
		}

		factura.getLineas().addAll(lineasFactura);
		recalcularTotales(factura);

		FacturaV2 guardada = facturaRepo.save(factura);

		servicios.forEach(s -> s.setFacturaV2Id(guardada.getId()));
		lineas.forEach(l -> l.setFacturaV2Id(guardada.getId()));

		return mapResponse(guardada);
	}

	@Transactional
	public void cancelarBorrador(Long facturaId) {
		String empresa = empresaActual();
		FacturaV2 factura = cargarFactura(facturaId, empresa);

		comprobarBorradorEditable(factura);

		List<Long> servicioIds = factura.getLineas().stream().filter(l -> "SERVICIO".equals(l.getTipoOrigen()))
				.map(LineaFacturaV2::getOrigenId).toList();

		if (!servicioIds.isEmpty()) {
			servicioRepo.findByEmpresaAndIdIn(empresa, servicioIds).forEach(s -> {
				if (facturaId.equals(s.getFacturaV2Id())) {
					s.setFacturaV2Id(null);
				}
			});
		}

		List<Long> lineaIds = factura.getLineas().stream().filter(l -> "ALBARAN_LINEA".equals(l.getTipoOrigen()))
				.map(LineaFacturaV2::getOrigenId).toList();

		if (!lineaIds.isEmpty()) {
			lineaRepo.findAllById(lineaIds).forEach(l -> {
				if (empresa.equals(l.getEmpresa()) && facturaId.equals(l.getFacturaV2Id())) {
					l.setFacturaV2Id(null);
				}
			});
		}

		facturaRepo.delete(factura);
	}

	@Transactional
	public FacturaV2Response emitir(Long facturaId) {
		String empresa = empresaActual();
		FacturaV2 factura = cargarFactura(facturaId, empresa);

		// La emisión local solo admite borradores sin vínculo remoto.
		comprobarBorradorEditable(factura);

		validarReservasSiguenVivas(empresa, factura);
		recalcularTotales(factura);

		factura.setHashEmision(generarHashEmision(factura));
		factura.setEstado("EMITIDA");

		return mapResponse(facturaRepo.save(factura));
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public FacturaV2 reservarEmisionFacturaDirecta(Long facturaId) {
		String empresa = empresaActual();
		FacturaV2 factura = cargarFactura(facturaId, empresa);

		entityManager.refresh(factura, LockModeType.PESSIMISTIC_WRITE);

		if (!"BORRADOR".equalsIgnoreCase(factura.getEstado())) {
			throw new IllegalStateException("La factura no está en BORRADOR");
		}

		if (!tieneTexto(factura.getFacturaDirectaId()) || !factura.getFacturaDirectaId().startsWith("inv_")) {
			throw new IllegalStateException("Primero debes preparar el borrador en FacturaDirecta");
		}

		if (!"BORRADOR_REMOTO".equals(factura.getVerifactuEstado())) {
			throw new IllegalStateException(
					"Existe una operación pendiente. Consulta su resultado " + "antes de volver a emitir");
		}

		Cliente cliente = factura.getCliente();

		if (cliente == null || !tieneTexto(cliente.getFacturaDirectaContactId())
				|| !cliente.getFacturaDirectaContactId().startsWith("con_")
				|| !tieneTexto(factura.getFacturaDirectaCompanyId())
				|| !factura.getFacturaDirectaCompanyId().equals(cliente.getFacturaDirectaCompanyId())) {
			throw new IllegalStateException("El cliente no tiene un vínculo remoto válido");
		}

		if (factura.getFechaEmision() == null || factura.getLineas().isEmpty()) {
			throw new IllegalStateException("La factura debe tener fecha y líneas");
		}

		for (LineaFacturaV2 linea : factura.getLineas()) {
			validarLinea(linea);

			if (Double.compare(linea.getIvaPct(), 21.0) != 0) {
				throw new IllegalStateException("Esta prueba solo admite IVA del 21%");
			}
		}

		validarReservasSiguenVivas(empresa, factura);
		recalcularTotales(factura);

		factura.setHashEmision(generarHashEmision(factura));
		factura.setVerifactuEstado("EMISION_EN_CURSO");

		facturaRepo.saveAndFlush(factura);

		cliente.getNombreApellidos();
		factura.getLineas().forEach(l -> l.getDescripcion());

		return factura;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public FacturaV2Response confirmarEmisionFacturaDirecta(Long facturaId, String remotoId, String numeroRemoto,
			String qrUrl) {
		String empresa = empresaActual();
		if (!tieneTexto(numeroRemoto)) {
			throw new IllegalStateException("No se recibió el número definitivo");
		}

		FacturaV2 factura = cargarFactura(facturaId, empresa);
		entityManager.refresh(factura, LockModeType.PESSIMISTIC_WRITE);

		if (remotoId == null || !remotoId.equals(factura.getFacturaDirectaId())) {
			throw new IllegalStateException("El ID remoto no coincide");
		}

		boolean yaEmitida = "EMITIDA".equalsIgnoreCase(factura.getEstado())
				|| "PAGADA".equalsIgnoreCase(factura.getEstado());
		if (yaEmitida) {
			if (!numeroRemoto.equals(factura.getFacturaDirectaNumero())) {
				throw new IllegalStateException("El número remoto difiere de la emisión guardada");
			}
		} else {
			if (!"BORRADOR".equalsIgnoreCase(factura.getEstado())
					|| !"EMISION_EN_CURSO".equals(factura.getVerifactuEstado())) {
				throw new IllegalStateException("La factura no tiene una emisión reservada");
			}
			factura.setFacturaDirectaNumero(numeroRemoto);
			factura.setEstado("EMITIDA");
			// La emisión no equivale a aceptación confirmada de la AEAT.
			factura.setVerifactuEstado("PENDIENTE_CONFIRMACION_AEAT");
		}

		if (tieneTexto(qrUrl)) {
			if (tieneTexto(factura.getVerifactuQrUrl()) && !qrUrl.equals(factura.getVerifactuQrUrl())) {
				throw new IllegalStateException("El QR remoto difiere del guardado");
			}
			factura.setVerifactuQrUrl(qrUrl);
		}
		// Conserva PAGADA, el resultado AEAT y cualquier QR ya guardado.
		return mapResponse(facturaRepo.saveAndFlush(factura));
	}

	@Transactional
	public FacturaV2Response marcarComoPagada(Long facturaId) {
		FacturaV2 factura = cargarFactura(facturaId, empresaActual());

		entityManager.refresh(factura, LockModeType.PESSIMISTIC_WRITE);

		if (!"EMITIDA".equalsIgnoreCase(factura.getEstado())) {
			throw new IllegalStateException("Solo se puede marcar como pagada una factura EMITIDA");
		}

		factura.setEstado("PAGADA");

		return mapResponse(facturaRepo.save(factura));
	}

	@Transactional
	public FacturaV2Response crearBorradorDesdeTrabajo(Long trabajoId) {
		if (trabajoId == null) {
			throw new IllegalArgumentException("Trabajo no válido");
		}

		var albaran = albaranClienteService.crearDesdeTrabajo(trabajoId);

		if (albaran == null || albaran.getId() == null) {
			throw new IllegalStateException("No se pudo crear el albarán del trabajo");
		}

		albaran = albaranClienteService.confirmar(albaran.getId());

		if (albaran.getCliente() == null || albaran.getCliente().getId() == null) {
			throw new IllegalStateException("El albarán no tiene un cliente válido");
		}

		if (albaran.getLineas() == null || albaran.getLineas().isEmpty()) {
			throw new IllegalStateException("El albarán creado no contiene líneas");
		}

		LineaAlbaranCliente linea = albaran.getLineas().get(0);

		if (linea == null || linea.getId() == null) {
			throw new IllegalStateException("No se pudo identificar la línea del albarán");
		}

		CrearFacturaV2Request req = new CrearFacturaV2Request(albaran.getCliente().getId(), "A", List.of(),
				List.of(linea.getId()));

		return crearBorrador(req);
	}

	private String empresaActual() {
		String empresa = TenantContext.get();

		if (!tieneTexto(empresa)) {
			throw new IllegalStateException("Empresa no seleccionada");
		}

		return empresa;
	}

	private FacturaV2 cargarFactura(Long facturaId, String empresa) {
		if (facturaId == null) {
			throw new IllegalArgumentException("Factura no indicada");
		}

		return facturaRepo.findByIdAndEmpresa(facturaId, empresa)
				.orElseThrow(() -> new IllegalArgumentException("Factura no encontrada en la empresa seleccionada"));
	}

	private void comprobarBorradorEditable(FacturaV2 factura) {
		entityManager.refresh(factura, LockModeType.PESSIMISTIC_WRITE);

		if (!"BORRADOR".equalsIgnoreCase(factura.getEstado())) {
			throw new IllegalStateException("La factura ya no está en BORRADOR");
		}

		if (tieneTexto(factura.getVerifactuEstado()) || tieneTexto(factura.getFacturaDirectaId())
				|| tieneTexto(factura.getFacturaDirectaCompanyId())) {
			throw new IllegalStateException("La factura tiene una operación con FacturaDirecta. "
					+ "No se puede modificar, eliminar ni emitir localmente");
		}
	}

	private int siguienteNumero(String empresa, String serie) {
		ContadorFacturaV2 contador = contadorRepo.findByEmpresaAndSerie(empresa, serie).orElseGet(() -> {
			ContadorFacturaV2 nuevo = new ContadorFacturaV2();
			nuevo.setEmpresa(empresa);
			nuevo.setSerie(serie);
			nuevo.setSiguienteNumero(1);
			return contadorRepo.save(nuevo);
		});

		int actual = contador.getSiguienteNumero();
		contador.setSiguienteNumero(actual + 1);
		contadorRepo.save(contador);

		return actual;
	}

	private void validarLinea(LineaFacturaV2 linea) {
		if (linea == null) {
			throw new IllegalArgumentException("Línea no válida");
		}

		validarDatosLinea(linea.getDescripcion(), linea.getCantidad(), linea.getPrecioUnitario(),
				linea.getDescuentoPct(), linea.getIvaPct());
	}

	private void validarDatosLinea(String descripcion, Double cantidad, Double precio, Double descuento, Double iva) {

		if (!tieneTexto(descripcion)) {
			throw new IllegalArgumentException("La descripción de la línea no puede estar vacía");
		}

		if (cantidad == null || !Double.isFinite(cantidad) || cantidad <= 0) {
			throw new IllegalArgumentException("La cantidad debe ser un número mayor que 0");
		}

		if (precio == null || !Double.isFinite(precio) || precio < 0) {
			throw new IllegalArgumentException("El precio unitario debe ser un número no negativo");
		}

		if (descuento == null || !Double.isFinite(descuento) || descuento < 0 || descuento > 100) {
			throw new IllegalArgumentException("El descuento debe estar entre 0 y 100");
		}

		if (iva == null || !Double.isFinite(iva) || iva < 0) {
			throw new IllegalArgumentException("El IVA debe ser un número no negativo");
		}
	}

	private void recalcularTotales(FacturaV2 factura) {
		double base = 0;
		double iva = 0;
		double total = 0;

		for (LineaFacturaV2 linea : factura.getLineas()) {
			validarLinea(linea);
			linea.recalcular();

			base += linea.getSubtotal();
			iva += linea.getSubtotal() * (linea.getIvaPct() / 100.0);
			total += linea.getTotalLinea();
		}

		if (!Double.isFinite(base) || !Double.isFinite(iva) || !Double.isFinite(total)) {
			throw new IllegalArgumentException("Los importes de la factura no son válidos");
		}

		factura.setBaseImponible(round2(base));
		factura.setIvaTotal(round2(iva));
		factura.setTotal(round2(total));
	}

	private double round2(double valor) {
		return Math.round(valor * 100.0) / 100.0;
	}

	private void validarReservasSiguenVivas(String empresa, FacturaV2 factura) {

		Long facturaId = factura.getId();

		List<Long> servicioIds = factura.getLineas().stream().filter(l -> "SERVICIO".equals(l.getTipoOrigen()))
				.map(LineaFacturaV2::getOrigenId).toList();

		if (!servicioIds.isEmpty()) {
			List<ServicioCliente> servicios = servicioRepo.findByEmpresaAndIdIn(empresa, servicioIds);

			if (servicios.size() != servicioIds.size()) {
				throw new IllegalStateException("Servicios no válidos");
			}

			boolean invalidos = servicios.stream().anyMatch(s -> !facturaId.equals(s.getFacturaV2Id())
					|| s.getCliente() == null || !factura.getCliente().getId().equals(s.getCliente().getId()));

			if (invalidos) {
				throw new IllegalStateException(
						"Hay servicios no reservados por esta factura " + "o que pertenecen a otro cliente");
			}
		}

		List<Long> lineaIds = factura.getLineas().stream().filter(l -> "ALBARAN_LINEA".equals(l.getTipoOrigen()))
				.map(LineaFacturaV2::getOrigenId).toList();

		if (!lineaIds.isEmpty()) {
			List<LineaAlbaranCliente> lineas = lineaRepo.findAllById(lineaIds);

			if (lineas.size() != lineaIds.size()) {
				var encontrados = lineas.stream().map(LineaAlbaranCliente::getId).toList();

				throw new IllegalStateException("Líneas de albarán no válidas. " + "IDs solicitados: " + lineaIds
						+ ". IDs encontrados: " + encontrados);
			}

			boolean invalidas = lineas.stream()
					.anyMatch(l -> !empresa.equals(l.getEmpresa()) || !facturaId.equals(l.getFacturaV2Id()));

			if (invalidas) {
				throw new IllegalStateException("Hay líneas no reservadas por esta factura");
			}
		}
	}

	private FacturaV2Response mapResponse(FacturaV2 factura) {
		List<LineaFacturaV2Response> lineas = factura.getLineas().stream()
				.map(l -> new LineaFacturaV2Response(l.getId(), l.getTipoOrigen(), l.getOrigenId(), l.getDescripcion(),
						l.getCantidad(), l.getPrecioUnitario(), l.getDescuentoPct(), l.getSubtotal(), l.getIvaPct(),
						l.getTotalLinea()))
				.toList();

		return new FacturaV2Response(factura.getId(), factura.getEmpresa(), factura.getSerie(), factura.getNumero(),
				factura.getFechaEmision(), factura.getEstado(), factura.getBaseImponible(), factura.getIvaTotal(),
				factura.getTotal(), toClienteDTO(factura.getCliente()), toEmisorDTO(factura.getEmpresa()), lineas,
				factura.getFacturaDirectaCompanyId(), factura.getFacturaDirectaId(), factura.getFacturaDirectaNumero(),
				factura.getVerifactuEstado(), factura.getVerifactuQrUrl());
	}

	private ClienteDTO toClienteDTO(Cliente cliente) {
		if (cliente == null) {
			return null;
		}

		return new ClienteDTO(cliente.getId(), cliente.getNombreApellidos(), cliente.getCifDni(),
				cliente.getDireccion(), cliente.getCodigoPostal(), cliente.getPoblacion(), cliente.getProvincia(),
				cliente.getTelefono(), cliente.getEmail());
	}

	private EmpresaEmisoraDTO toEmisorDTO(String empresa) {
		String emp = empresa == null ? "" : empresa.trim().toUpperCase(java.util.Locale.ROOT);

		if ("ARGASA".equals(emp)) {
			return new EmpresaEmisoraDTO("Argasa Garrido S.L.", "B36879617", "Rúa Pintor Laxeiro Nº15 Bajo", "36211",
					"Vigo", "Pontevedra", "607472159", "argasaluis@gmail.com");
		}

		if ("ELECTROLUGA".equals(emp) || "LUGA".equals(emp)) {
			return new EmpresaEmisoraDTO("Electrodomesticos Luis Garrido S.L.", "B-42722389",
					"Calle Pintor Laxeiro, 15", "36211", "Vigo", "Pontevedra", "986 234 946",
					"electroluga@empresa.com");
		}

		return new EmpresaEmisoraDTO(emp, "", "", "", "", "", "", "");
	}

	private String generarHashEmision(FacturaV2 factura) {
		StringBuilder sb = new StringBuilder();

		sb.append(factura.getEmpresa()).append("|").append(factura.getSerie()).append("|").append(factura.getNumero())
				.append("|").append(factura.getFechaEmision()).append("|");

		factura.getLineas()
				.forEach(l -> sb.append(l.getTipoOrigen()).append(":").append(l.getOrigenId()).append(":")
						.append(l.getDescripcion()).append(":").append(l.getCantidad()).append(":")
						.append(l.getPrecioUnitario()).append(":").append(l.getDescuentoPct()).append(":")
						.append(l.getIvaPct()).append("|"));

		return sha256(sb.toString());
	}

	private String sha256(String input) {
		try {
			var md = java.security.MessageDigest.getInstance("SHA-256");
			byte[] hash = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));

			StringBuilder hex = new StringBuilder();

			for (byte b : hash) {
				hex.append(String.format("%02x", b));
			}

			return hex.toString();
		} catch (java.security.NoSuchAlgorithmException e) {
			throw new IllegalStateException("Error generando hash", e);
		}
	}

	private boolean tieneTexto(String valor) {
		return valor != null && !valor.isBlank();
	}
}
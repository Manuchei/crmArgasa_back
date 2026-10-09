package com.empresa.crm.services;

import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.empresa.crm.entities.Cliente;
import com.empresa.crm.entities.facturacionV2.FacturaV2;
import com.empresa.crm.tenant.TenantContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class FacturaDirectaService {

	private final String argasaCompanyId;
	private final String electrolugaCompanyId;
	private final RestClient argasaClient;
	private final RestClient electrolugaClient;

	public FacturaDirectaService(@Value("${facturadirecta.base-url}") String baseUrl,
			@Value("${facturadirecta.company-id}") String argasaCompanyId,
			@Value("${facturadirecta.api-key}") String argasaApiKey,
			@Value("${facturadirecta.electroluga.company-id:}") String electrolugaCompanyId,
			@Value("${facturadirecta.electroluga.api-key:}") String electrolugaApiKey) {

		this.argasaCompanyId = textoContacto(argasaCompanyId);
		this.electrolugaCompanyId = textoContacto(electrolugaCompanyId);

		validarSandbox(this.argasaCompanyId, "ARGASA");

		if (!this.electrolugaCompanyId.isBlank()) {
			validarSandbox(this.electrolugaCompanyId, "ELECTROLUGA");

			if (this.argasaCompanyId.equals(this.electrolugaCompanyId)) {
				throw new IllegalStateException("ARGASA y ELECTROLUGA deben utilizar sandboxes distintos");
			}
		}

		this.argasaClient = crearClient(baseUrl, argasaApiKey, "ARGASA");

		this.electrolugaClient = this.electrolugaCompanyId.isBlank() ? null
				: crearClient(baseUrl, electrolugaApiKey, "ELECTROLUGA");
	}

	private RestClient crearClient(String baseUrl, String apiKey, String empresa) {
		String clave = textoContacto(apiKey);

		if (clave.isBlank()) {
			throw new IllegalStateException("API key de FacturaDirecta no configurada para " + empresa);
		}

		var factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Duration.ofSeconds(10));
		factory.setReadTimeout(Duration.ofSeconds(20));

		return RestClient.builder().baseUrl(textoContacto(baseUrl)).requestFactory(factory)
				.defaultHeader("facturadirecta-api-key", clave).defaultHeader("Accept", "application/json").build();
	}

	private void validarSandbox(String company, String empresa) {
		if (!company.startsWith("com_sandbox_")) {
			throw new IllegalStateException("Esta integración requiere un sandbox para " + empresa);
		}
	}

	private String normalizarEmpresa(String empresa) {
		return textoContacto(empresa).toUpperCase(Locale.ROOT);
	}

	public String obtenerCompanyIdPrueba(String empresa) {
		switch (normalizarEmpresa(empresa)) {
		case "ARGASA":
			return argasaCompanyId;

		case "ELECTROLUGA":
			if (electrolugaCompanyId.isBlank() || electrolugaClient == null) {
				throw new IllegalStateException("Sandbox no configurado para ELECTROLUGA");
			}
			return electrolugaCompanyId;

		default:
			throw new IllegalStateException("Empresa no habilitada para FacturaDirecta");
		}
	}

	private RestClient clientePrueba(String empresa) {
		obtenerCompanyIdPrueba(empresa);

		return "ARGASA".equals(normalizarEmpresa(empresa)) ? argasaClient : electrolugaClient;
	}

	private String empresaActual() {
		String empresa = normalizarEmpresa(TenantContext.get());

		if (empresa.isBlank()) {
			throw new IllegalStateException("Empresa no seleccionada");
		}

		obtenerCompanyIdPrueba(empresa);
		return empresa;
	}

	public JsonNode consultarFacturas() {
		String empresa = empresaActual();
		String company = obtenerCompanyIdPrueba(empresa);

		return clientePrueba(empresa).get().uri(builder -> builder.path("/{companyId}/invoices")
				.queryParam("draft", "all").queryParam("limit", 5).build(company)).retrieve().body(JsonNode.class);
	}

	public JsonNode consultarFactura(String facturaId) {
		return consultarFacturaPrueba(empresaActual(), facturaId);
	}

	// Método antiguo de prueba con un contacto fijo de ARGASA.
	// La integración normal utiliza crearBorradorDesdeFacturaPrueba.
	public JsonNode crearBorradorPrueba() {
		String empresa = empresaActual();

		if (!"ARGASA".equals(empresa)) {
			throw new IllegalStateException("La prueba con contacto fijo solo está disponible para ARGASA. "
					+ "En ELECTROLUGA prepara un borrador desde una factura de NovexApp");
		}

		String company = obtenerCompanyIdPrueba(empresa);
		String fecha = LocalDate.now(ZoneId.of("Europe/Madrid")).toString();

		var linea = new LinkedHashMap<String, Object>();
		linea.put("account", "700000");
		linea.put("quantity", 1);
		linea.put("unitPrice", 100);
		linea.put("discount", 0);
		linea.put("discountRate", 0);
		linea.put("lineTotal", 100);
		linea.put("tax", List.of("S_IVA_21"));
		linea.put("text", "Prueba de integración NovexApp");
		linea.put("verifactu", Map.of("ClaveRegimen", "01", "CalificacionOperacion", "S1"));

		var main = crearMain("con_68e50e6a-65b0-4272-9bec-36a60f7e57ee", fecha, List.of(linea));

		return clientePrueba(empresa).post().uri("/{companyId}/invoices", company)
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("content", Map.of("type", "invoice", "main", main))).retrieve().body(JsonNode.class);
	}

	public JsonNode emitirBorradorPrueba(String facturaId) {
		return emitirBorradorPrueba(empresaActual(), facturaId);
	}

	public JsonNode emitirBorradorPrueba(String empresa, String facturaId) {
		String company = obtenerCompanyIdPrueba(empresa);
		JsonNode respuesta = consultarFacturaPrueba(empresa, facturaId);

		if (respuesta == null) {
			throw new IllegalStateException("FacturaDirecta no devolvió la factura");
		}

		JsonNode factura = respuesta.path("content").isObject() ? respuesta.path("content") : respuesta;

		if (!"invoice".equals(factura.path("type").asText()) || !facturaId.equals(factura.path("uuid").asText())
				|| !factura.path("main").isObject()) {
			throw new IllegalStateException("La respuesta no contiene la factura esperada");
		}

		ObjectNode main = (ObjectNode) factura.path("main").deepCopy();

		if (!main.path("draft").isBoolean()) {
			throw new IllegalStateException("No se puede determinar si es borrador");
		}

		if (main.path("voided").asBoolean() || main.path("external").asBoolean()
				|| main.path("simplified").asBoolean()) {
			throw new IllegalStateException("Esta prueba no admite facturas anuladas, externas o simplificadas");
		}

		if (!main.path("draft").booleanValue()) {
			return respuesta;
		}

		main.put("draft", false);

		return clientePrueba(empresa).put().uri("/{companyId}/invoices/{id}", company, facturaId)
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("content", Map.of("type", "invoice", "main", main))).retrieve().body(JsonNode.class);
	}

	public JsonNode buscarContactosPrueba(Cliente cliente) {
		if (cliente == null) {
			throw new IllegalArgumentException("Cliente no indicado");
		}

		String empresa = cliente.getEmpresa();
		String company = obtenerCompanyIdPrueba(empresa);
		String nif = normalizarNif(cliente.getCifDni());

		if (nif.isBlank()) {
			throw new IllegalArgumentException("El cliente debe tener NIF");
		}

		return clientePrueba(empresa).get().uri(builder -> builder.path("/{companyId}/contacts")
				.queryParam("fiscalId", nif).queryParam("limit", 2).build(company)).retrieve().body(JsonNode.class);
	}

	public JsonNode crearContactoPrueba(Cliente cliente) {
		if (cliente == null) {
			throw new IllegalArgumentException("Cliente no indicado");
		}

		String empresa = cliente.getEmpresa();
		String company = obtenerCompanyIdPrueba(empresa);
		String nif = normalizarNif(cliente.getCifDni());
		String nombre = textoContacto(cliente.getNombreApellidos());

		if (nombre.isBlank() || nif.isBlank()) {
			throw new IllegalArgumentException("El cliente debe tener nombre fiscal y NIF");
		}

		var main = new LinkedHashMap<String, Object>();
		main.put("name", nombre);
		main.put("fiscalId", nif);
		main.put("fiscalIdCountry", "ES");
		main.put("country", "ES");
		main.put("currency", "EUR");
		main.put("accounts", Map.of("client", "430000", "clientCredit", "438000"));

		ponerDatoContacto(main, "address", cliente.getDireccion());
		ponerDatoContacto(main, "zipcode", cliente.getCodigoPostal());
		ponerDatoContacto(main, "city", cliente.getPoblacion());
		ponerDatoContacto(main, "region", cliente.getProvincia());
		ponerDatoContacto(main, "email", cliente.getEmail());
		ponerDatoContacto(main, "phone", cliente.getTelefono());

		return clientePrueba(empresa).post().uri("/{companyId}/contacts", company)
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("content", Map.of("type", "contact", "main", main))).retrieve().body(JsonNode.class);
	}

	public String obtenerOCrearContactoPrueba(Cliente cliente) {
		if (cliente == null) {
			throw new IllegalArgumentException("Cliente no indicado");
		}

		String company = obtenerCompanyIdPrueba(cliente.getEmpresa());
		String nif = normalizarNif(cliente.getCifDni());
		String nombre = textoContacto(cliente.getNombreApellidos());

		if (nif.isBlank() || nombre.isBlank()) {
			throw new IllegalArgumentException("El cliente debe tener nombre fiscal y NIF");
		}

		String companyGuardada = textoContacto(cliente.getFacturaDirectaCompanyId());
		String idGuardado = textoContacto(cliente.getFacturaDirectaContactId());

		if (!companyGuardada.isBlank() && !company.equals(companyGuardada)) {
			throw new IllegalStateException("El cliente está vinculado a otra empresa de FacturaDirecta");
		}

		JsonNode respuesta = buscarContactosPrueba(cliente);

		if (respuesta == null || !respuesta.path("items").isArray()) {
			throw new IllegalStateException("Respuesta de contactos inesperada");
		}

		JsonNode items = respuesta.path("items");

		if (items.size() > 1) {
			throw new IllegalStateException("Hay varios contactos con ese NIF. Revisa FacturaDirecta");
		}

		JsonNode contacto;

		if (items.size() == 1) {
			contacto = items.get(0).path("content");
		} else {
			JsonNode creado = crearContactoPrueba(cliente);

			if (creado == null) {
				throw new IllegalStateException("FacturaDirecta no devolvió el contacto creado");
			}

			contacto = creado.path("content");
		}

		JsonNode main = contacto.path("main");

		if (!nif.equals(normalizarNif(main.path("fiscalId").asText()))) {
			throw new IllegalStateException("El NIF del contacto de FacturaDirecta no coincide");
		}

		if (!normalizarNombreContacto(nombre).equals(normalizarNombreContacto(main.path("name").asText()))) {
			throw new IllegalStateException("Existe un contacto con ese NIF, pero su nombre fiscal "
					+ "no coincide. Revisa el cliente en ambos sistemas");
		}

		if (main.path("accounts").path("client").asText().isBlank()) {
			throw new IllegalStateException("El contacto de FacturaDirecta no está habilitado como cliente");
		}

		String contactoId = contacto.path("uuid").asText();

		if (!contactoId.startsWith("con_")) {
			throw new IllegalStateException("FacturaDirecta no devolvió un ID de contacto válido");
		}

		if (!idGuardado.isBlank() && !contactoId.equals(idGuardado)) {
			throw new IllegalStateException("El contacto encontrado no coincide con el vínculo guardado");
		}

		return contactoId;
	}

	public JsonNode crearBorradorDesdeFacturaPrueba(FacturaV2 factura, String contactoId) {

		if (factura == null || factura.getId() == null) {
			throw new IllegalArgumentException("Factura no indicada");
		}

		String empresa = factura.getEmpresa();
		String company = obtenerCompanyIdPrueba(empresa);

		if (!"BORRADOR".equalsIgnoreCase(factura.getEstado())) {
			throw new IllegalStateException("La factura debe estar en BORRADOR");
		}

		if (!textoContacto(factura.getFacturaDirectaId()).isBlank()) {
			throw new IllegalStateException("La factura ya tiene un ID de FacturaDirecta");
		}

		if (contactoId == null || !contactoId.startsWith("con_")) {
			throw new IllegalArgumentException("Contacto no válido");
		}

		if (factura.getFechaEmision() == null || factura.getLineas() == null || factura.getLineas().isEmpty()) {
			throw new IllegalArgumentException("La factura debe tener fecha y líneas");
		}

		var lineas = new ArrayList<Map<String, Object>>();

		for (var linea : factura.getLineas()) {
			if (linea == null || textoContacto(linea.getDescripcion()).isBlank() || linea.getCantidad() == null
					|| !Double.isFinite(linea.getCantidad()) || linea.getCantidad() <= 0
					|| linea.getPrecioUnitario() == null || !Double.isFinite(linea.getPrecioUnitario())
					|| linea.getPrecioUnitario() < 0 || linea.getDescuentoPct() == null
					|| !Double.isFinite(linea.getDescuentoPct()) || linea.getDescuentoPct() < 0
					|| linea.getDescuentoPct() > 100) {
				throw new IllegalArgumentException("Hay una línea con datos no válidos");
			}

			if (linea.getIvaPct() == null || Double.compare(linea.getIvaPct(), 21.0) != 0) {
				throw new IllegalArgumentException("Esta prueba solo admite IVA del 21%");
			}

			linea.recalcular();

			double bruto = linea.getCantidad() * linea.getPrecioUnitario();
			double descuentoImporte = Math.round((bruto - linea.getSubtotal()) * 100.0) / 100.0;

			var datos = new LinkedHashMap<String, Object>();
			datos.put("account", "700000");
			datos.put("quantity", linea.getCantidad());
			datos.put("unitPrice", linea.getPrecioUnitario());
			datos.put("discount", descuentoImporte);
			datos.put("discountRate", linea.getDescuentoPct() / 100.0);
			datos.put("lineTotal", linea.getSubtotal());
			datos.put("tax", List.of("S_IVA_21"));
			datos.put("text", linea.getDescripcion().trim());
			datos.put("verifactu", Map.of("ClaveRegimen", "01", "CalificacionOperacion", "S1"));

			lineas.add(datos);
		}

		var main = crearMain(contactoId, factura.getFechaEmision().toString(), lineas);

		String referencia = "novexapp-" + factura.getEmpresa() + "-factura-" + factura.getId();

		return clientePrueba(empresa).post().uri("/{companyId}/invoices", company)
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("content", Map.of("type", "invoice", "main", main), "tags", List.of(referencia)))
				.retrieve().body(JsonNode.class);
	}

	private Map<String, Object> crearMain(String contactoId, String fecha, List<Map<String, Object>> lineas) {

		var main = new LinkedHashMap<String, Object>();
		main.put("contact", contactoId);
		main.put("date", fecha);
		main.put("dueDate", fecha);
		main.put("currency", "EUR");
		main.put("exchangeRate", 1);
		main.put("docNumber", Map.of("series", "F##"));
		main.put("fiscalPosition", "ind");
		main.put("account", "430000");
		main.put("theme", "thm_master");
		main.put("draft", true);
		main.put("voided", false);
		main.put("simplified", false);
		main.put("verifactu", Map.of("TipoFactura", "F1"));
		main.put("lines", lineas);
		return main;
	}

	public JsonNode buscarFacturaPrueba(FacturaV2 factura) {
		if (factura == null || factura.getId() == null) {
			throw new IllegalArgumentException("Factura no indicada");
		}

		String empresa = factura.getEmpresa();
		String company = obtenerCompanyIdPrueba(empresa);
		String referencia = "novexapp-" + factura.getEmpresa() + "-factura-" + factura.getId();

		JsonNode respuesta = clientePrueba(empresa).get()
				.uri(builder -> builder.path("/{companyId}/invoices").queryParam("draft", "all")
						.queryParam("allTheseTags", referencia).queryParam("limit", 2).build(company))
				.retrieve().body(JsonNode.class);

		if (respuesta == null || !respuesta.path("items").isArray()) {
			throw new IllegalStateException("Respuesta de búsqueda de facturas inesperada");
		}

		JsonNode items = respuesta.path("items");

		if (items.size() > 1) {
			throw new IllegalStateException("Hay varias facturas de FacturaDirecta vinculadas "
					+ "a esta factura de NovexApp. Revisa los duplicados");
		}

		if (items.isEmpty()) {
			return null;
		}

		JsonNode encontrada = items.get(0);
		String id = encontrada.path("content").path("uuid").asText();

		if (!id.startsWith("inv_")) {
			throw new IllegalStateException("La factura encontrada no contiene un ID válido");
		}

		return encontrada;
	}

	public JsonNode consultarFacturaPrueba(String empresa, String facturaId) {

		String company = obtenerCompanyIdPrueba(empresa);

		if (facturaId == null || !facturaId.startsWith("inv_")) {
			throw new IllegalArgumentException("ID de factura no válido");
		}

		return clientePrueba(empresa).get().uri("/{companyId}/invoices/{id}", company, facturaId).retrieve()
				.body(JsonNode.class);
	}

	public JsonNode consultarEndpointsWebhookPrueba(String empresa) {
		String company = obtenerCompanyIdPrueba(empresa);

		return clientePrueba(empresa).get().uri("/{companyId}/webhooks/endpoints", company).retrieve()
				.body(JsonNode.class);
	}

	public JsonNode consultarEventosVerifactuPrueba(String empresa) {
		String company = obtenerCompanyIdPrueba(empresa);

		return clientePrueba(empresa).get()
				.uri(builder -> builder.path("/{companyId}/webhooks/events")
						.queryParam("type", "invoice.verifactu_sent").queryParam("limit", 10).build(company))
				.retrieve().body(JsonNode.class);
	}

	private String normalizarNif(String nif) {
		return textoContacto(nif).replaceAll("[\\s.-]", "").toUpperCase(Locale.ROOT);
	}

	private String normalizarNombreContacto(String nombre) {
		return Normalizer.normalize(textoContacto(nombre), Normalizer.Form.NFD).replaceAll("\\p{M}", "")
				.replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
	}

	private String textoContacto(String valor) {
		return valor == null ? "" : valor.trim();
	}

	private void ponerDatoContacto(Map<String, Object> datos, String campo, String valor) {

		String texto = textoContacto(valor);

		if (!texto.isBlank()) {
			datos.put(campo, texto);
		}
	}
}
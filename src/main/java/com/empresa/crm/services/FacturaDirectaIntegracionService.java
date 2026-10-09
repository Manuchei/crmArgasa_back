package com.empresa.crm.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.empresa.crm.dto.facturacionv2.FacturaV2Response;
import com.empresa.crm.entities.facturacionV2.FacturaV2;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class FacturaDirectaIntegracionService {

	private final FacturaDirectaService api;
	private final FacturaDirectaContactoService contactos;
	private final FacturaDirectaEstadoService estados;
	private final FacturacionV2Service facturacion;

	public FacturaDirectaIntegracionService(FacturaDirectaService api, FacturaDirectaContactoService contactos,
			FacturaDirectaEstadoService estados, FacturacionV2Service facturacion) {

		this.api = api;
		this.contactos = contactos;
		this.estados = estados;
		this.facturacion = facturacion;
	}

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public JsonNode prepararBorradorPrueba(Long facturaId) {
		FacturaV2 factura = estados.cargarFactura(facturaId);
		String company = api.obtenerCompanyIdPrueba(factura.getEmpresa());

		if (!"BORRADOR".equalsIgnoreCase(factura.getEstado())) {
			throw new IllegalStateException("La factura debe estar en BORRADOR");
		}

		if (tieneTexto(factura.getFacturaDirectaCompanyId()) && !company.equals(factura.getFacturaDirectaCompanyId())) {
			throw new IllegalStateException("La factura está vinculada a otra empresa remota");
		}

		if (factura.getCliente() == null) {
			throw new IllegalStateException("La factura no tiene cliente");
		}

		String contactoId = contactos.obtenerContactoPrueba(factura.getCliente().getId());

		if (tieneTexto(factura.getFacturaDirectaId())) {
			JsonNode remota = api.consultarFacturaPrueba(factura.getEmpresa(), factura.getFacturaDirectaId());

			comprobarContacto(remota, contactoId);
			return remota;
		}

		JsonNode encontrada = api.buscarFacturaPrueba(factura);

		if (encontrada != null) {
			comprobarContacto(encontrada, contactoId);

			estados.guardarVinculo(facturaId, encontrada.path("content").path("uuid").asText());

			return encontrada;
		}

		if (tieneTexto(factura.getVerifactuEstado())) {
			throw new IllegalStateException("Hay un intento previo sin resultado confirmado. "
					+ "Revisa FacturaDirecta antes de volver a crear");
		}

		estados.reservarCreacion(facturaId);
		factura = estados.cargarFactura(facturaId);

		if (!contactoId.equals(factura.getCliente().getFacturaDirectaContactId())) {
			throw new IllegalStateException("El contacto del cliente ha cambiado");
		}

		JsonNode creada = api.crearBorradorDesdeFacturaPrueba(factura, contactoId);

		comprobarContacto(creada, contactoId);

		estados.guardarVinculo(facturaId, creada.path("content").path("uuid").asText());

		return creada;
	}

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public FacturaV2Response emitirPrueba(Long facturaId) {
		FacturaV2 factura = estados.cargarFactura(facturaId);
		String company = api.obtenerCompanyIdPrueba(factura.getEmpresa());

		if (!company.equals(factura.getFacturaDirectaCompanyId())) {
			throw new IllegalStateException("La factura no pertenece al sandbox configurado");
		}

		if (!tieneTexto(factura.getFacturaDirectaId())) {
			throw new IllegalStateException("Primero pulsa Preparar en FacturaDirecta (prueba)");
		}

		if (factura.getCliente() == null) {
			throw new IllegalStateException("La factura no tiene cliente");
		}

		String contactoId = factura.getCliente().getFacturaDirectaContactId();

		if (!tieneTexto(contactoId) || !company.equals(factura.getCliente().getFacturaDirectaCompanyId())) {
			throw new IllegalStateException("El cliente no tiene un vínculo válido con este sandbox");
		}

		JsonNode remota = api.consultarFacturaPrueba(factura.getEmpresa(), factura.getFacturaDirectaId());

		System.out.println("=== LINEAS FACTURADIRECTA ===");
		System.out.println(remota.path("content").path("main").path("lines").toPrettyString());

		System.out.println("=== LINEAS NOVEXAPP ===");
		factura.getLineas().forEach(linea -> {
			System.out.println("Descripción: " + linea.getDescripcion());
			System.out.println("Cantidad: " + linea.getCantidad());
			System.out.println("Precio: " + linea.getPrecioUnitario());
			System.out.println("Descuento: " + linea.getDescuentoPct());
			System.out.println("IVA: " + linea.getIvaPct());
			System.out.println("Subtotal: " + linea.getSubtotal());
		});

		comprobarFacturaParaEmision(factura, remota, contactoId);

		JsonNode main = remota.path("content").path("main");

		if ("EMITIDA".equalsIgnoreCase(factura.getEstado()) || "PAGADA".equalsIgnoreCase(factura.getEstado())) {

			if (main.path("draft").asBoolean()) {
				throw new IllegalStateException("La factura local figura emitida, " + "pero la remota es borrador");
			}

			return guardarResultadoEmision(facturaId, remota);
		}

		if (!"BORRADOR".equalsIgnoreCase(factura.getEstado())) {
			throw new IllegalStateException("La factura no puede emitirse en su estado actual");
		}

		// Recuperar una emisión anterior sin enviar otra petición PUT.
		if (!main.path("draft").asBoolean()) {
			if (!"EMISION_EN_CURSO".equals(factura.getVerifactuEstado())) {
				throw new IllegalStateException(
						"La factura se emitió fuera de este flujo. " + "Hay que revisar su sincronización");
			}

			return guardarResultadoEmision(facturaId, remota);
		}

		if ("EMISION_EN_CURSO".equals(factura.getVerifactuEstado())) {
			throw new IllegalStateException(
					"El intento anterior aún no está confirmado. " + "No se enviará otra emisión automáticamente");
		}

		// La reserva se confirma antes de la petición HTTP.
		factura = facturacion.reservarEmisionFacturaDirecta(facturaId);

		if (!company.equals(factura.getFacturaDirectaCompanyId())
				|| !company.equals(factura.getCliente().getFacturaDirectaCompanyId())) {
			throw new IllegalStateException("El vínculo remoto ha cambiado");
		}

		remota = api.consultarFacturaPrueba(factura.getEmpresa(), factura.getFacturaDirectaId());

		comprobarFacturaParaEmision(factura, remota, factura.getCliente().getFacturaDirectaContactId());

		if (!remota.path("content").path("main").path("draft").asBoolean()) {
			return guardarResultadoEmision(facturaId, remota);
		}

		JsonNode emitida = api.emitirBorradorPrueba(factura.getFacturaDirectaId());

		comprobarFacturaParaEmision(factura, emitida, factura.getCliente().getFacturaDirectaContactId());

		return guardarResultadoEmision(facturaId, emitida);
	}

	private FacturaV2Response guardarResultadoEmision(Long facturaId, JsonNode respuesta) {
		if (respuesta == null) {
			throw new IllegalStateException("FacturaDirecta no devolvió el resultado");
		}

		JsonNode content = respuesta.path("content");
		JsonNode main = content.path("main");
		String remotoId = content.path("uuid").asText();

		if (!"invoice".equals(content.path("type").asText()) || !remotoId.startsWith("inv_")) {
			throw new IllegalStateException("FacturaDirecta no devolvió una factura válida");
		}
		if (!main.path("draft").isBoolean() || main.path("draft").booleanValue() || main.path("voided").asBoolean()) {
			throw new IllegalStateException("FacturaDirecta no ha confirmado una factura definitiva vigente");
		}

		JsonNode docNumber = main.path("docNumber");
		if (!docNumber.path("number").isIntegralNumber() || docNumber.path("number").asLong() <= 0
				|| docNumber.path("formattedSeries").asText().isBlank()) {
			throw new IllegalStateException("FacturaDirecta no devolvió una numeración definitiva válida");
		}

		String numero = docNumber.path("formattedSeries").asText() + " " + docNumber.path("number").asText();
		String qrUrl = content.path("meta").path("verifactu").path("qrUrl").asText("");

		// Una factura definitiva puede tener el QR pendiente de generación.
		return facturacion.confirmarEmisionFacturaDirecta(facturaId, remotoId, numero,
				tieneTexto(qrUrl) ? qrUrl : null);
	}

	private void comprobarFacturaParaEmision(FacturaV2 factura, JsonNode respuesta, String contactoId) {

		comprobarContacto(respuesta, contactoId);

		JsonNode content = respuesta.path("content");
		JsonNode main = content.path("main");

		if (!"invoice".equals(content.path("type").asText()) || factura.getFacturaDirectaId() == null
				|| !factura.getFacturaDirectaId().equals(content.path("uuid").asText())) {

			throw new IllegalStateException("La factura remota no coincide");
		}

		if (!main.path("draft").isBoolean()) {
			throw new IllegalStateException("No se puede determinar el estado del borrador remoto");
		}

		if (main.path("external").asBoolean() || main.path("simplified").asBoolean()) {

			throw new IllegalStateException("Esta prueba solo admite facturas completas no externas");
		}

		if (!"EUR".equals(main.path("currency").asText()) || !mismoNumero(1.0, main.path("exchangeRate"))
				|| factura.getFechaEmision() == null
				|| !factura.getFechaEmision().toString().equals(main.path("date").asText())
				|| !"F##".equals(main.path("docNumber").path("series").asText())
				|| !"F1".equals(main.path("verifactu").path("TipoFactura").asText())) {

			throw new IllegalStateException("La fecha, moneda, cambio, serie o tipo de factura remota no coincide");
		}

		JsonNode lineas = main.path("lines");

		if (!lineas.isArray() || factura.getLineas() == null || lineas.size() != factura.getLineas().size()
				|| lineas.isEmpty()) {

			throw new IllegalStateException("Las líneas remotas no coinciden con NovexApp");
		}

		List<JsonNode> pendientes = new ArrayList<>();
		lineas.forEach(pendientes::add);

		for (var local : factura.getLineas()) {

			if (local == null || local.getDescripcion() == null || local.getDescripcion().isBlank()
					|| local.getCantidad() == null || !Double.isFinite(local.getCantidad()) || local.getCantidad() <= 0
					|| local.getPrecioUnitario() == null || !Double.isFinite(local.getPrecioUnitario())
					|| local.getPrecioUnitario() < 0 || local.getDescuentoPct() == null
					|| !Double.isFinite(local.getDescuentoPct()) || local.getDescuentoPct() < 0
					|| local.getDescuentoPct() > 100 || local.getSubtotal() == null
					|| !Double.isFinite(local.getSubtotal())) {

				throw new IllegalStateException("Hay una línea local con datos no válidos");
			}

			double bruto = local.getCantidad() * local.getPrecioUnitario();

			double subtotalEsperado = Math.round(bruto * (1.0 - local.getDescuentoPct() / 100.0) * 100.0) / 100.0;

			if (!Double.isFinite(bruto) || Double.compare(subtotalEsperado, local.getSubtotal()) != 0) {

				throw new IllegalStateException(
						"El subtotal local no corresponde al precio y descuento: " + local.getDescripcion());
			}

			double tasaDescuento = local.getDescuentoPct() / 100.0;

			double descuentoImporte = Math.round((bruto - subtotalEsperado) * 100.0) / 100.0;

			int encontrada = -1;

			for (int i = 0; i < pendientes.size(); i++) {
				JsonNode remota = pendientes.get(i);

				if (local.getDescripcion().trim().equals(remota.path("text").asText())
						&& mismoNumero(local.getCantidad(), remota.path("quantity"))
						&& mismoNumero(local.getPrecioUnitario(), remota.path("unitPrice"))
						&& mismoNumero(tasaDescuento, remota.path("discountRate"))
						&& mismoNumero(descuentoImporte, remota.path("discount"))
						&& mismoNumero(subtotalEsperado, remota.path("lineTotal")) && local.getIvaPct() != null
						&& Double.compare(local.getIvaPct(), 21.0) == 0 && remota.path("tax").isArray()
						&& remota.path("tax").size() == 1 && "S_IVA_21".equals(remota.path("tax").get(0).asText())
						&& "01".equals(remota.path("verifactu").path("ClaveRegimen").asText())
						&& "S1".equals(remota.path("verifactu").path("CalificacionOperacion").asText())
						&& remota.path("verifactu").path("OperacionExenta").asText().isBlank()) {

					encontrada = i;
					break;
				}
			}

			if (encontrada < 0) {
				throw new IllegalStateException("La línea remota no coincide: " + local.getDescripcion()
						+ ". Revisa cantidad, precio, descuento, subtotal e impuestos");
			}

			pendientes.remove(encontrada);
		}
	}

	private void comprobarContacto(JsonNode respuesta, String contactoId) {

		if (!tieneTexto(contactoId) || respuesta == null
				|| !contactoId.equals(respuesta.path("content").path("main").path("contact").asText())) {
			throw new IllegalStateException("La factura remota no corresponde " + "al contacto esperado");
		}

		if (respuesta.path("content").path("main").path("voided").asBoolean()) {
			throw new IllegalStateException("La factura remota está anulada");
		}
	}

	private boolean mismoNumero(Double local, JsonNode remoto) {
		if (local == null || !Double.isFinite(local) || remoto == null || !remoto.isNumber()) {
			return false;
		}

		return BigDecimal.valueOf(local).compareTo(remoto.decimalValue()) == 0;
	}

	private boolean tieneTexto(String valor) {
		return valor != null && !valor.isBlank();
	}

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public JsonNode diagnosticarVerifactuPrueba(Long facturaId) {
		FacturaV2 factura = estados.cargarFactura(facturaId);

		String company = api.obtenerCompanyIdPrueba(factura.getEmpresa());

		if (!company.equals(factura.getFacturaDirectaCompanyId())) {
			throw new IllegalStateException("La factura no pertenece al sandbox configurado");
		}

		if (!tieneTexto(factura.getFacturaDirectaId())) {
			throw new IllegalStateException("La factura no está vinculada a FacturaDirecta");
		}

		JsonNode remota = api.consultarFacturaPrueba(factura.getEmpresa(), factura.getFacturaDirectaId());

		if (remota == null || !factura.getFacturaDirectaId().equals(remota.path("content").path("uuid").asText())) {
			throw new IllegalStateException("La factura remota no coincide");
		}

		com.fasterxml.jackson.databind.node.ObjectNode resultado = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance
				.objectNode();

		resultado.set("verifactu", remota.path("content").path("meta").path("verifactu"));

		resultado.set("endpoints", api.consultarEndpointsWebhookPrueba(factura.getEmpresa()));

		resultado.set("eventos", api.consultarEventosVerifactuPrueba(factura.getEmpresa()));

		return resultado;
	}
}
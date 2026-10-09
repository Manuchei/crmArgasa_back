package com.empresa.crm.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.empresa.crm.entities.facturacionV2.FacturaV2;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

@Service
public class FacturaDirectaWebhookService {

    private final ObjectMapper objectMapper;
    private final EntityManager entityManager;
    private final FacturaDirectaService api;

    private final String argasaCompanyId;
    private final String argasaSecret;
    private final String electrolugaCompanyId;
    private final String electrolugaSecret;

    public FacturaDirectaWebhookService(
            ObjectMapper objectMapper,
            EntityManager entityManager,
            FacturaDirectaService api,
            @Value("${facturadirecta.company-id}") String argasaCompanyId,
            @Value("${facturadirecta.webhook-secret:}") String argasaSecret,
            @Value("${facturadirecta.electroluga.company-id:}") String electrolugaCompanyId,
            @Value("${facturadirecta.electroluga.webhook-secret:}") String electrolugaSecret) {

        this.objectMapper = objectMapper;
        this.entityManager = entityManager;
        this.api = api;

        this.argasaCompanyId = texto(argasaCompanyId);
        this.argasaSecret = texto(argasaSecret);
        this.electrolugaCompanyId = texto(electrolugaCompanyId);
        this.electrolugaSecret = texto(electrolugaSecret);

        if (!this.electrolugaCompanyId.isBlank()
                && this.argasaCompanyId.equals(this.electrolugaCompanyId)) {
            throw new IllegalStateException(
                    "ARGASA y ELECTROLUGA deben usar empresas remotas distintas");
        }
    }

    @Transactional
    public void procesar(byte[] cuerpo, String firma, String timestamp) {
        if (cuerpo == null || cuerpo.length == 0) {
            throw error(HttpStatus.BAD_REQUEST, "Cuerpo vacío");
        }

        JsonNode evento;

        try {
            evento = objectMapper.readTree(cuerpo);
        } catch (java.io.IOException ex) {
            throw error(HttpStatus.BAD_REQUEST, "JSON no válido");
        }

        if (evento == null || !evento.isObject()) {
            throw error(HttpStatus.BAD_REQUEST, "Evento no válido");
        }

        // Leer company_id solo para seleccionar la clave.
        // No se consulta ni modifica ninguna factura antes de verificar la firma.
        String company = evento.path("company_id").asText();
        String empresa;
        String secret;

        if (argasaCompanyId.equals(company)) {
            empresa = "ARGASA";
            secret = argasaSecret;
        } else if (!electrolugaCompanyId.isBlank()
                && electrolugaCompanyId.equals(company)) {
            empresa = "ELECTROLUGA";
            secret = electrolugaSecret;
        } else {
            throw error(HttpStatus.FORBIDDEN, "Empresa remota no válida");
        }

        comprobarFirma(cuerpo, firma, timestamp, secret);

        if (!company.equals(api.obtenerCompanyIdPrueba(empresa))) {
            throw error(HttpStatus.FORBIDDEN, "Configuración de empresa no válida");
        }

        JsonNode livemode = evento.path("livemode");

        if (!livemode.isBoolean() || livemode.booleanValue()) {
            throw error(
                    HttpStatus.FORBIDDEN,
                    "Este receptor solo admite eventos del sandbox");
        }

        String eventoId = evento.path("id").asText();

        if (!eventoId.startsWith("whe_")) {
            throw error(HttpStatus.BAD_REQUEST, "ID de evento no válido");
        }

        if (!"invoice.verifactu_sent".equals(evento.path("type").asText())) {
            return;
        }

        JsonNode documento = evento.path("data").path("object");

        if (documento.path("content").isObject()) {
            documento = documento.path("content");
        }

        String remotoId = documento.path("uuid").asText();

        if (!"invoice".equals(documento.path("type").asText())
                || !remotoId.startsWith("inv_")) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "El evento no contiene una factura reconocible");
        }

        JsonNode main = documento.path("main");

        if (!main.path("draft").isBoolean()
                || main.path("draft").booleanValue()
                || main.path("voided").asBoolean()) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "La confirmación no corresponde a una factura vigente");
        }

        if (!"mode_verifactu".equals(
                documento.path("meta").path("verifactu").path("mode").asText())) {
            throw error(HttpStatus.BAD_REQUEST, "Modo VeriFactu no válido");
        }

        JsonNode resultado = documento.path("verifactu_result");

        if (!resultado.isObject()) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "El evento no contiene el resultado de VeriFactu");
        }

        String operacion = resultado.path("tipoOperation").asText();
        String estadoRegistro = resultado.path("estadoRegistro").asText();

        if (!"alta".equalsIgnoreCase(operacion)) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "El resultado no corresponde al alta de la factura");
        }

        String nuevoEstado;

        switch (estadoRegistro) {
            case "Correcto":
                nuevoEstado = "ACEPTADA_AEAT";
                break;

            case "AceptadoConErrores":
                nuevoEstado = "ACEPTADA_AEAT_CON_ERRORES";
                break;

            case "Incorrecto":
                nuevoEstado = "RECHAZADA_AEAT";
                break;

            default:
                throw error(
                        HttpStatus.BAD_REQUEST,
                        "Resultado VeriFactu no reconocido: " + estadoRegistro);
        }

        List<FacturaV2> coincidencias = entityManager.createQuery("""
                select f
                from FacturaV2 f
                where f.empresa = :empresa
                  and f.facturaDirectaCompanyId = :company
                  and f.facturaDirectaId = :remotoId
                """, FacturaV2.class)
                .setParameter("empresa", empresa)
                .setParameter("company", company)
                .setParameter("remotoId", remotoId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();

        // No modificar documentos que no estén vinculados a NovexApp.
        if (coincidencias.isEmpty()) {
            return;
        }

        if (coincidencias.size() != 1) {
            throw error(
                    HttpStatus.CONFLICT,
                    "Hay varios vínculos locales para la factura remota");
        }

        FacturaV2 factura = coincidencias.get(0);

        // Si el webhook llega antes de terminar la emisión local,
        // responder con error temporal para permitir un reintento.
        if (!"EMITIDA".equalsIgnoreCase(factura.getEstado())
                && !"PAGADA".equalsIgnoreCase(factura.getEstado())) {
            throw error(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "La emisión local todavía no está confirmada");
        }

        String numero = main.path("docNumber").path("formattedSeries").asText()
                + " " + main.path("docNumber").path("number").asText();

        if (!numero.equals(factura.getFacturaDirectaNumero())) {
            throw error(
                    HttpStatus.CONFLICT,
                    "El número remoto no coincide con la factura local");
        }

        // Reenviar el mismo resultado no cambia EMITIDA/PAGADA.
        factura.setVerifactuEstado(nuevoEstado);
        entityManager.flush();
    }

    private void comprobarFirma(
            byte[] cuerpo,
            String firma,
            String timestamp,
            String secret) {

        if (secret.isBlank()) {
            throw error(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "El secreto del webhook no está configurado para esta empresa");
        }

        if (firma == null || timestamp == null) {
            throw error(HttpStatus.UNAUTHORIZED, "Faltan datos de la firma");
        }

        long instante;

        try {
            instante = Long.parseLong(timestamp);
        } catch (NumberFormatException ex) {
            throw error(HttpStatus.UNAUTHORIZED, "Timestamp no válido");
        }

        long ahora = Instant.now().getEpochSecond();

        if (instante < ahora - 300 || instante > ahora + 300) {
            throw error(HttpStatus.UNAUTHORIZED, "Firma caducada");
        }

        String timestampFirma = null;
        List<String> firmas = new ArrayList<>();

        for (String parte : firma.split(",")) {
            String valor = parte.trim();

            if (valor.startsWith("t=")) {
                timestampFirma = valor.substring(2);
            } else if (valor.startsWith("v1=")) {
                firmas.add(valor.substring(3));
            }
        }

        if (!timestamp.equals(timestampFirma) || firmas.isEmpty()) {
            throw error(HttpStatus.UNAUTHORIZED, "Firma no válida");
        }

        if (!secret.startsWith("whsec_")
                || !secret.substring("whsec_".length())
                        .matches("(?:[0-9a-fA-F]{2})+")) {
            throw error(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Formato del secreto del webhook no válido");
        }

        // Mantener el formato de clave comprobado con ARGASA:
        // texto UTF-8 del secreto sin el prefijo whsec_.
        String claveFirma = secret.substring("whsec_".length());
        byte[] esperada;

        try {
            Mac mac = Mac.getInstance("HmacSHA256");

            mac.init(new SecretKeySpec(
                    claveFirma.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"));

            mac.update((timestamp + ".").getBytes(StandardCharsets.UTF_8));
            esperada = mac.doFinal(cuerpo);

        } catch (java.security.GeneralSecurityException ex) {
            throw new IllegalStateException(
                    "No se pudo verificar la firma", ex);
        }

        boolean coincide = false;

        for (String candidata : firmas) {
            if (candidata.matches("[0-9a-fA-F]{64}")) {
                coincide |= MessageDigest.isEqual(
                        esperada,
                        HexFormat.of().parseHex(candidata));
            }
        }

        if (!coincide) {
            throw error(HttpStatus.UNAUTHORIZED, "Firma no válida");
        }
    }

    private String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private ResponseStatusException error(HttpStatus estado, String mensaje) {
        return new ResponseStatusException(estado, mensaje);
    }
}
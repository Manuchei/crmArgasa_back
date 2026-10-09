package com.empresa.crm.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.empresa.crm.services.FacturaDirectaWebhookService;

@RestController
@RequestMapping("/api/webhooks/facturadirecta")
public class FacturaDirectaWebhookController {

	private final FacturaDirectaWebhookService webhookService;

	public FacturaDirectaWebhookController(FacturaDirectaWebhookService webhookService) {
		this.webhookService = webhookService;
	}

	@PostMapping
	public ResponseEntity<Void> recibir(@RequestHeader(name = "Webhook-Signature", required = false) String firma,
			@RequestHeader(name = "Webhook-Timestamp", required = false) String timestamp, @RequestBody byte[] cuerpo) {

		webhookService.procesar(cuerpo, firma, timestamp);
		return ResponseEntity.noContent().build();
	}
}
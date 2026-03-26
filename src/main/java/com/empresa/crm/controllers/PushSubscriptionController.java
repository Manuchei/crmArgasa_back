package com.empresa.crm.controllers;

import java.util.Map;

import org.springframework.web.bind.annotation.*;

import com.empresa.crm.dto.PushSubscriptionRequestDTO;
import com.empresa.crm.services.PushSubscriptionService;

@RestController
@RequestMapping("/api/push")
public class PushSubscriptionController {

	private final PushSubscriptionService service;

	public PushSubscriptionController(PushSubscriptionService service) {
		this.service = service;
	}

	@PostMapping("/subscribe")
	public void subscribe(@RequestBody PushSubscriptionRequestDTO dto) {
		service.saveSubscription(dto);
	}

	@PostMapping("/unsubscribe")
	public void unsubscribe(@RequestBody Map<String, String> body) {
		service.deleteByEndpoint(body.get("endpoint"));
	}

	@GetMapping("/public-key")
	public Map<String, String> getPublicKey() {
		return Map.of("publicKey", System.getProperty("app.push.public-key", ""));
	}
}
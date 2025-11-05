package com.empresa.crm.controllers;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.empresa.crm.dto.ResumenAnualDTO;
import com.empresa.crm.services.InformeAnualService;

@RestController
@RequestMapping("/api/informes")
@CrossOrigin(origins = "http://localhost:4200")
public class InformeAnualController {

	private final InformeAnualService informeAnualService;

	public InformeAnualController(InformeAnualService informeAnualService) {
		this.informeAnualService = informeAnualService;
	}

	@GetMapping("/anual/{empresa}/{year}")
	public ResumenAnualDTO generarResumenAnual(@PathVariable String empresa, @PathVariable int year) {
		return informeAnualService.generarResumenAnual(empresa, year);
	}
}

package com.empresa.crm.controllers;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.empresa.crm.dto.ResumenMensualDTO;
import com.empresa.crm.services.InformeMensualService;

@RestController
@RequestMapping("/api/informes")
@CrossOrigin(origins = "http://localhost:4200")
public class InformeMensualController {

	private final InformeMensualService informeMensualService;

	public InformeMensualController(InformeMensualService informeMensualService) {
		this.informeMensualService = informeMensualService;
	}

	@GetMapping("/mensual/{empresa}/{year}/{month}")
	public ResumenMensualDTO generarResumen(@PathVariable String empresa, @PathVariable int year,
			@PathVariable int month) {
		return informeMensualService.generarResumen(empresa, year, month);
	}
}

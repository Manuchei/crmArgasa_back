package com.empresa.crm.controllers;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.empresa.crm.dto.ResumenComparativoDTO;
import com.empresa.crm.services.InformeComparativoService;

@RestController
@RequestMapping("/api/informes")
@CrossOrigin(origins = "http://localhost:4200")
public class InformeComparativoController {

	private final InformeComparativoService informeComparativoService;

	public InformeComparativoController(InformeComparativoService informeComparativoService) {
		this.informeComparativoService = informeComparativoService;
	}

	@GetMapping("/comparativo/{year}")
	public ResumenComparativoDTO generarComparativoAnual(@PathVariable int year) {
		return informeComparativoService.generarComparativoAnual(year);
	}
}
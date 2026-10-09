package com.empresa.crm.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.empresa.crm.dto.facturacionv2.ActualizarFacturaV2Request;
import com.empresa.crm.dto.facturacionv2.CrearFacturaV2Request;
import com.empresa.crm.dto.facturacionv2.FacturaV2Response;
import com.empresa.crm.dto.facturacionv2.PendientesFacturacionDTO;
import com.empresa.crm.services.FacturaDirectaIntegracionService;
import com.empresa.crm.services.FacturacionV2PendientesService;
import com.empresa.crm.services.FacturacionV2Service;
import com.fasterxml.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/facturacion-v2")
@CrossOrigin(origins = "http://localhost:4200")
public class FacturacionV2Controller {

	private final FacturacionV2PendientesService pendientesService;
	private final FacturacionV2Service facturacionV2Service;
	private final FacturaDirectaIntegracionService facturaDirectaIntegracion;

	public FacturacionV2Controller(FacturacionV2PendientesService pendientesService,
			FacturacionV2Service facturacionV2Service, FacturaDirectaIntegracionService facturaDirectaIntegracion) {

		this.pendientesService = pendientesService;
		this.facturacionV2Service = facturacionV2Service;
		this.facturaDirectaIntegracion = facturaDirectaIntegracion;
	}

	@GetMapping("/pendientes/cliente/{clienteId}")
	public PendientesFacturacionDTO pendientes(@PathVariable Long clienteId) {

		return pendientesService.obtenerPendientes(clienteId);
	}

	@PostMapping("/facturas")
	public FacturaV2Response crearFactura(@RequestBody CrearFacturaV2Request req) {

		return facturacionV2Service.crearBorrador(req);
	}

	@PostMapping("/facturas/trabajos/{trabajoId}")
	public FacturaV2Response crearFacturaDesdeTrabajo(@PathVariable Long trabajoId) {

		return facturacionV2Service.crearBorradorDesdeTrabajo(trabajoId);
	}

	@PutMapping("/facturas/{id:\\d+}")
	public ResponseEntity<FacturaV2Response> actualizarFactura(@PathVariable Long id,
			@RequestBody ActualizarFacturaV2Request req) {

		return ResponseEntity.ok(facturacionV2Service.actualizarBorrador(id, req));
	}

	@DeleteMapping("/facturas/{id:\\d+}")
	public ResponseEntity<?> cancelar(@PathVariable Long id) {
		facturacionV2Service.cancelarBorrador(id);

		return ResponseEntity.ok().build();
	}

	// Emisión local: bloqueada para facturas vinculadas a FacturaDirecta.
	@PostMapping("/facturas/{id:\\d+}/emitir")
	public FacturaV2Response emitir(@PathVariable Long id) {
		return facturacionV2Service.emitir(id);
	}

	@GetMapping("/facturas")
	public List<FacturaV2Response> listarFacturas(@RequestParam(required = false) String estado,
			@RequestParam(required = false) Long clienteId) {

		return facturacionV2Service.listarFacturas(clienteId, estado);
	}

	@GetMapping("/facturas/{id:\\d+}")
	public ResponseEntity<FacturaV2Response> getFacturaById(@PathVariable Long id) {

		return ResponseEntity.ok(facturacionV2Service.getFacturaById(id));
	}

	@PostMapping("/facturas/{id:\\d+}/marcar-pagada")
	public FacturaV2Response marcarComoPagada(@PathVariable Long id) {
		return facturacionV2Service.marcarComoPagada(id);
	}

	@PostMapping("/facturas/{id:\\d+}/facturadirecta/borrador")
	public JsonNode prepararBorradorFacturaDirecta(@PathVariable Long id) {

		return facturaDirectaIntegracion.prepararBorradorPrueba(id);
	}

	@PostMapping("/facturas/{id:\\d+}/facturadirecta/emitir")
	public FacturaV2Response emitirFacturaDirecta(@PathVariable Long id) {

		return facturaDirectaIntegracion.emitirPrueba(id);
	}

	@GetMapping("/facturas/{id:\\d+}/facturadirecta/diagnostico")
	public JsonNode diagnosticarVerifactu(@PathVariable Long id) {
		return facturaDirectaIntegracion.diagnosticarVerifactuPrueba(id);
	}
}
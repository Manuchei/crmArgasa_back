package com.empresa.crm.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.empresa.crm.dto.facturacionv2.CrearFacturaV2Request;
import com.empresa.crm.dto.facturacionv2.FacturaV2Response;
import com.empresa.crm.dto.facturacionv2.PendientesFacturacionDTO;
import com.empresa.crm.services.FacturacionV2PendientesService;
import com.empresa.crm.services.FacturacionV2Service;

@RestController
@RequestMapping("/api/facturacion-v2")
@CrossOrigin(origins = "http://localhost:4200")
public class FacturacionV2Controller {

  private final FacturacionV2PendientesService pendientesService;
  private final FacturacionV2Service facturacionV2Service;

  public FacturacionV2Controller(FacturacionV2PendientesService pendientesService,
                                 FacturacionV2Service facturacionV2Service) {
    this.pendientesService = pendientesService;
    this.facturacionV2Service = facturacionV2Service;
  }

  @GetMapping("/pendientes/cliente/{clienteId}")
  public PendientesFacturacionDTO pendientes(@PathVariable Long clienteId) {
    return pendientesService.obtenerPendientes(clienteId);
  }

  @PostMapping("/facturas")
  public FacturaV2Response crearFactura(@RequestBody CrearFacturaV2Request req) {
    return facturacionV2Service.crearBorrador(req);
  }

  @DeleteMapping("/facturas/{id:\\d+}")
  public void cancelar(@PathVariable Long id) {
    facturacionV2Service.cancelarBorrador(id);
  }

  @PostMapping("/facturas/{id:\\d+}/emitir")
  public FacturaV2Response emitir(@PathVariable Long id) {
    return facturacionV2Service.emitir(id);
  }

  @GetMapping("/facturas")
  public List<FacturaV2Response> listarFacturas(
      @RequestParam(required = false) String estado,
      @RequestParam(required = false) Long clienteId
  ) {
    return facturacionV2Service.listarFacturas(clienteId, estado);
  }

  // ✅ ESTE ES EL QUE TE FALTABA / ESTABA MAL
  // Angular llama a: /api/facturacion-v2/facturas/{id}
  @GetMapping("/facturas/{id:\\d+}")
  public ResponseEntity<FacturaV2Response> getFacturaById(@PathVariable Long id) {
    return ResponseEntity.ok(facturacionV2Service.getFacturaById(id));
  }

  // (Opcional) si existe este endpoint en tu app, déjalo explícito para evitar choques:
  // @GetMapping("/facturas/proximas")
  // public List<FacturaV2Response> proximas() {
  //   return facturacionV2Service.proximas();
  // }
}

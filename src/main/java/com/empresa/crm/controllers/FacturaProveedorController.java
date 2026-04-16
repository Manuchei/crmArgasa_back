package com.empresa.crm.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.empresa.crm.entities.FacturaProveedor;
import com.empresa.crm.services.FacturaProveedorService;
import com.empresa.crm.tenant.TenantContext;

@RestController
@RequestMapping("/api/facturas")
@CrossOrigin(origins = "http://localhost:4200")
public class FacturaProveedorController {

	private final FacturaProveedorService facturaService;

	public FacturaProveedorController(FacturaProveedorService facturaService) {
		this.facturaService = facturaService;
	}

	@GetMapping
	public List<FacturaProveedor> listarTodas() {
		return facturaService.findAll();
	}

	@GetMapping("/{id}")
	public FacturaProveedor obtenerPorId(@PathVariable Long id) {
		return facturaService.findById(id);
	}

	@GetMapping("/proveedor/{proveedorId}")
	public List<FacturaProveedor> listarPorProveedor(@PathVariable Long proveedorId) {
		return facturaService.findByProveedor(proveedorId);
	}

	@PostMapping("/generar/{proveedorId}")
	public FacturaProveedor generar(@PathVariable Long proveedorId) {
		String empresa = TenantContext.get();

		if (empresa == null || empresa.isBlank()) {
			throw new RuntimeException("Empresa no seleccionada (TenantContext vacío).");
		}

		return facturaService.generarFactura(proveedorId, empresa);
	}

	@PostMapping("/generar-desde-albaran/{albaranId}")
	public FacturaProveedor generarDesdeAlbaran(@PathVariable Long albaranId) {
		String empresa = TenantContext.get();

		if (empresa == null || empresa.isBlank()) {
			throw new RuntimeException("Empresa no seleccionada (TenantContext vacío).");
		}

		return facturaService.generarFacturaDesdeAlbaran(albaranId);
	}

	@PutMapping("/{facturaId}")
	public FacturaProveedor guardarBorrador(@PathVariable Long facturaId,
			@RequestBody FacturaProveedor facturaEditada) {
		return facturaService.guardarBorrador(facturaId, facturaEditada);
	}

	@PutMapping("/emitir/{facturaId}")
	public FacturaProveedor emitir(@PathVariable Long facturaId) {
		return facturaService.emitirFactura(facturaId);
	}

	@PutMapping("/pagar/{facturaId}")
	public FacturaProveedor pagar(@PathVariable Long facturaId) {
		return facturaService.marcarComoPagada(facturaId);
	}

	@PutMapping("/numero-proveedor/{facturaId}")
	public FacturaProveedor actualizarNumeroFacturaProveedor(@PathVariable Long facturaId,
			@RequestParam String numeroFacturaProveedor) {
		return facturaService.actualizarNumeroFacturaProveedor(facturaId, numeroFacturaProveedor);
	}

	@GetMapping("/empresa/{empresa}")
	public List<FacturaProveedor> listarPorEmpresa(@PathVariable String empresa) {
		return facturaService.findByEmpresa(empresa);
	}

	@org.springframework.web.bind.annotation.DeleteMapping("/{facturaId}")
	public void eliminarBorrador(@PathVariable Long facturaId) {
		facturaService.eliminarBorrador(facturaId);
	}
}
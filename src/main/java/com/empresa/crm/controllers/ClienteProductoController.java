package com.empresa.crm.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.empresa.crm.dto.AddProductoRequest;
import com.empresa.crm.entities.Cliente;
import com.empresa.crm.entities.Producto;
import com.empresa.crm.entities.Trabajo;
import com.empresa.crm.repositories.ClienteRepository;
import com.empresa.crm.repositories.ProductoRepository;
import com.empresa.crm.repositories.TrabajoRepository;
import com.empresa.crm.services.ClienteProductoService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;

@RestController
@RequestMapping("/api/clientes")
@CrossOrigin(origins = "http://localhost:4200")
public class ClienteProductoController {

	private final ClienteProductoService service;
	private final ClienteRepository clienteRepo;
	private final ProductoRepository productoRepo;
	private final TrabajoRepository trabajoRepo;

	public ClienteProductoController(ClienteProductoService service, ClienteRepository clienteRepo,
			ProductoRepository productoRepo, TrabajoRepository trabajoRepo) {
		this.service = service;
		this.clienteRepo = clienteRepo;
		this.productoRepo = productoRepo;
		this.trabajoRepo = trabajoRepo;
	}

	@PostMapping("/{clienteId}/productos/{productoId}")
	@Transactional
	public ResponseEntity<?> addProducto(@PathVariable Long clienteId, @PathVariable Long productoId,
			@RequestBody(required = false) AddProductoRequest body, HttpServletRequest request) {

		Cliente c = clienteRepo.findById(clienteId).orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

		Producto p = productoRepo.findById(productoId)
				.orElseThrow(() -> new RuntimeException("Producto no encontrado"));

		int cantidad = 1;
		double descuento = 0.0;
		double importePagado = 0.0;

		if (body != null) {
			if (body.getCantidad() != null)
				cantidad = body.getCantidad();
			if (body.getDescuento() != null)
				descuento = body.getDescuento();
			if (body.getImportePagado() != null)
				importePagado = body.getImportePagado();
		}

		if (cantidad <= 0)
			return ResponseEntity.badRequest().body("Cantidad inválida");
		if (descuento < 0 || descuento > 100)
			return ResponseEntity.badRequest().body("Descuento inválido (0..100)");
		if (importePagado < 0)
			return ResponseEntity.badRequest().body("Importe pagado inválido");

		if (p.getStock() < cantidad) {
			return ResponseEntity.badRequest().body("Sin stock suficiente");
		}

		// ✅ Empresa: header X-Empresa o del cliente
		String empresaHeader = request.getHeader("X-Empresa");
		String empresa = (empresaHeader != null && !empresaHeader.isBlank()) ? empresaHeader.trim() : c.getEmpresa();

		if (empresa == null || empresa.isBlank()) {
			return ResponseEntity.badRequest().body("Empresa no determinada");
		}

		// ✅ Seguridad extra: el producto debe ser de esa empresa
		// (si tu Producto tiene getEmpresa())
		if (p.getEmpresa() != null && !empresa.equalsIgnoreCase(p.getEmpresa())) {
			return ResponseEntity.badRequest().body("El producto no pertenece a la empresa seleccionada");
		}

		// 1) bajar stock
		p.setStock(p.getStock() - cantidad);
		productoRepo.save(p);

		// IVA configurable
		final double IVA = 0.21;

		// precio del producto sin IVA
		double precioBase = (p.getPrecioSinIva() != null) ? p.getPrecioSinIva() : 0.0;

		// precio con IVA
		double precioConIva = Math.round(precioBase * (1 + IVA) * 100.0) / 100.0;

		// 2) crear trabajo asociado
		Trabajo t = new Trabajo();
		t.setCliente(c);

		// ✅ CLAVE: guardar productoId para poder devolver stock al eliminar
		t.setProductoId(p.getId());

		// descripción: nombre del producto
		t.setDescripcion(p.getNombre());

		t.setUnidades(cantidad);
		t.setPrecioUnitario(precioConIva);
		t.setDescuento(descuento);

		// pagado inicial
		t.setImportePagado(importePagado);
		t.setPagado(false); // se recalcula en @PrePersist/@PreUpdate

		t.setEmpresa(empresa);

		// ✅ guardamos y devolvemos el trabajo creado
		Trabajo guardado = trabajoRepo.save(t);

		return ResponseEntity.ok(guardado);
	}
}

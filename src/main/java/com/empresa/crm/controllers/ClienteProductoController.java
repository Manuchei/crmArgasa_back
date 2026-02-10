package com.empresa.crm.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.empresa.crm.entities.Cliente;
import com.empresa.crm.entities.ClienteProducto;
import com.empresa.crm.entities.Producto;
import com.empresa.crm.entities.Trabajo;
import com.empresa.crm.repositories.ClienteRepository;
import com.empresa.crm.repositories.ProductoRepository;
import com.empresa.crm.services.ClienteProductoService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;

@RestController
@RequestMapping("/api/clientes")
@CrossOrigin
public class ClienteProductoController {

	private final  ClienteProductoService service;
	private final ClienteRepository clienteRepo;
	private final ProductoRepository productoRepo;
	
	public ClienteProductoController(ClienteProductoService service, ClienteRepository clienteRepo, ProductoRepository productoRepo 	) {
		this.service = service;
		this.clienteRepo = clienteRepo;
		this.productoRepo = productoRepo;

	}
	
	@PostMapping("/{clienteId}/productos/{productoId}")
	@Transactional
	public ResponseEntity<?> addProducto(@PathVariable Long clienteId,
	                                     @PathVariable Long productoId,
	                                     HttpServletRequest request) {

	    Cliente c = clienteRepo.findById(clienteId)
	            .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

	    Producto p = productoRepo.findById(productoId)
	            .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

	    if (p.getStock() ==  || p.getStock() <= 0) {
	        return ResponseEntity.badRequest().body("Sin stock");
	    }

	    // 1) bajar stock
	    p.setStock(p.getStock() - 1);
	    productoRepo.save(p);

	    // 2) crear trabajo asociado
	    Trabajo t = new Trabajo();
	    t.setCliente(c);
	    t.setDescripcion("Producto: " + p.getNombre());
	    t.setUnidades(1);

	    // si tu Producto NO tiene precio, deja 0, pero que sea coherente
	    t.setPrecioUnitario(0);
	    t.setDescuento(0);

	    // importe neto = unidades * precioUnitario * (1 - dto/100)
	    double neto = 1 * 0; // si metes precio, calcula aquí
	    t.setImporte(neto);

	    // ✅ MUY IMPORTANTE si en tu BD es NOT NULL:
	    t.setImportePagado(0.0);
	    t.setPagado(false);

	    // ✅ EMPRESA (usa cliente.empresa o header X-Empresa)
	    String empresaHeader = request.getHeader("X-Empresa");
	    String empresa = (empresaHeader != null && !empresaHeader.isBlank())
	            ? empresaHeader
	            : c.getEmpresa();

	    t.setEmpresa(empresa);

	    trabajoRepo.save(t);

	    return ResponseEntity.ok().build();
	}

	
}

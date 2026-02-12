package com.empresa.crm.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.empresa.crm.entities.Cliente;
import com.empresa.crm.entities.Producto;
import com.empresa.crm.entities.Proveedor;
import com.empresa.crm.entities.Trabajo;
import com.empresa.crm.repositories.ProductoRepository;
import com.empresa.crm.services.ClienteService;
import com.empresa.crm.services.ProveedorService;
import com.empresa.crm.services.TrabajoService;

import jakarta.transaction.Transactional;

@RestController
@RequestMapping("/api/trabajos")
@CrossOrigin(origins = "http://localhost:4200")
public class TrabajoController {

	private final TrabajoService trabajoService;
	private final ClienteService clienteService;
	private final ProveedorService proveedorService;
	private final ProductoRepository productoRepo;

	public TrabajoController(TrabajoService trabajoService, ClienteService clienteService,
			ProveedorService proveedorService, ProductoRepository productoRepo) {
		this.trabajoService = trabajoService;
		this.clienteService = clienteService;
		this.proveedorService = proveedorService;
		this.productoRepo = productoRepo;
	}

	// -------------------- CRUD GENERAL --------------------

	@GetMapping
	public List<Trabajo> listarTodos() {
		return trabajoService.findAll();
	}

	@GetMapping("/{id}")
	public Trabajo obtenerPorId(@PathVariable Long id) {
		return trabajoService.findById(id);
	}

	@PostMapping
	public Trabajo crear(@RequestBody Trabajo trabajo) {
		return trabajoService.save(trabajo);
	}

	@PutMapping("/{id}")
	public Trabajo actualizar(@PathVariable Long id, @RequestBody Trabajo trabajo) {
		trabajo.setId(id);
		return trabajoService.save(trabajo);
	}

	/**
	 * ✅ ELIMINAR TRABAJO Si el trabajo viene de un producto (productoId != null),
	 * devolvemos stock: stock += unidades
	 */
	@DeleteMapping("/{id}")
	@Transactional
	public void eliminar(@PathVariable Long id) {

		Trabajo trabajo = trabajoService.findById(id);
		if (trabajo == null) {
			throw new RuntimeException("Trabajo no encontrado con ID: " + id);
		}

		// ✅ devolver stock si procede
		Long productoId = trabajo.getProductoId();
		if (productoId != null) {
			Producto prod = productoRepo.findById(productoId)
					.orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + productoId));

			int unidades = (trabajo.getUnidades() != null && trabajo.getUnidades() > 0) ? trabajo.getUnidades() : 1;

			// getStock seguramente es int (no null). Si fuese Integer, esto también
			// funciona:
			int actual = prod.getStock();
			int nuevo = actual + unidades;

			prod.setStock(nuevo);
			productoRepo.save(prod);
		}

		trabajoService.deleteById(id);
	}

	// -------------------- FILTROS --------------------

	@GetMapping("/proveedor/{proveedorId}")
	public List<Trabajo> listarPorProveedor(@PathVariable Long proveedorId) {
		return trabajoService.findByProveedor(proveedorId);
	}

	@GetMapping("/pagado/{pagado}")
	public List<Trabajo> listarPorPago(@PathVariable boolean pagado) {
		return trabajoService.findByPagado(pagado);
	}

	@GetMapping("/cliente/{clienteId}")
	public List<Trabajo> listarPorCliente(@PathVariable Long clienteId) {
		return trabajoService.findByCliente(clienteId);
	}

	// -------------------- CLIENTES --------------------
	@PostMapping("/cliente/{clienteId}")
	public Trabajo crearTrabajoParaCliente(@PathVariable Long clienteId, @RequestBody Trabajo trabajo,
			@RequestHeader(value = "X-Empresa", required = false) String empresaHeader) {

		Cliente cliente = clienteService.findById(clienteId);

		if (cliente == null) {
			throw new RuntimeException("Cliente no encontrado con ID: " + clienteId);
		}

		// ✅ asociar cliente al trabajo
		trabajo.setCliente(cliente);

		// ✅ empresa obligatoria (NOT NULL en BD)
		String empresa = cliente.getEmpresa();

		if ((empresa == null || empresa.isBlank()) && empresaHeader != null && !empresaHeader.isBlank()) {
			empresa = empresaHeader.trim();
		}

		if (empresa == null || empresa.isBlank()) {
			throw new RuntimeException(
					"No se pudo determinar la empresa del trabajo (cliente sin empresa y sin header X-Empresa).");
		}

		trabajo.setEmpresa(empresa);

		return trabajoService.save(trabajo);
	}

	// -------------------- PROVEEDORES --------------------

	@PostMapping("/proveedor/{proveedorId}")
	public Trabajo crearTrabajoParaProveedor(@PathVariable Long proveedorId, @RequestBody Trabajo trabajo) {

		Proveedor proveedor = proveedorService.findById(proveedorId);

		if (proveedor == null) {
			throw new RuntimeException("Proveedor no encontrado con ID: " + proveedorId);
		}

		trabajo.setProveedor(proveedor);

		if (trabajo.getEmpresa() == null || trabajo.getEmpresa().isBlank()) {
			trabajo.setEmpresa(proveedor.getEmpresa());
		}

		trabajoService.save(trabajo);

		proveedor.getTrabajos().add(trabajo);
		proveedorService.save(proveedor);

		return trabajo;
	}

	@DeleteMapping("/proveedor/{trabajoId}")
	@Transactional
	public void eliminarTrabajoProveedor(@PathVariable Long trabajoId) {

		Trabajo trabajo = trabajoService.findById(trabajoId);

		if (trabajo == null) {
			throw new RuntimeException("Trabajo no encontrado con ID: " + trabajoId);
		}

		// ✅ también devolvemos stock si venía de producto
		Long productoId = trabajo.getProductoId();
		if (productoId != null) {
			Producto prod = productoRepo.findById(productoId)
					.orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + productoId));

			int unidades = (trabajo.getUnidades() != null && trabajo.getUnidades() > 0) ? trabajo.getUnidades() : 1;
			prod.setStock(prod.getStock() + unidades);
			productoRepo.save(prod);
		}

		Proveedor proveedor = trabajo.getProveedor();

		trabajoService.deleteById(trabajoId);

		proveedor.getTrabajos().remove(trabajo);
		proveedorService.save(proveedor);
	}
}

package com.empresa.crm.controllers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.empresa.crm.dto.AddProductoRequest;
import com.empresa.crm.dto.ClienteProductoCompradoDTO;
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
	public Trabajo addProductoCliente(Long clienteId, Long productoId, AddProductoRequest req, String empresa) {

		int cantidad = req.getCantidad() != null ? req.getCantidad() : 1;
		if (cantidad <= 0)
			cantidad = 1;

		Producto p = productoRepo.findById(productoId).orElseThrow(() -> new RuntimeException("Producto no existe"));

		if (!p.getEmpresa().equalsIgnoreCase(empresa)) {
			throw new RuntimeException("Producto no pertenece a la empresa activa");
		}

		int updated = productoRepo.decrementStockIfAvailable(productoId, cantidad);
		if (updated == 0) {
			throw new RuntimeException("Stock insuficiente");
		}

		Trabajo t = new Trabajo();
		t.setCliente(clienteRepo.getReferenceById(clienteId));
		t.setProductoId(productoId);
		t.setUnidades(cantidad);
		t.setDescuento(req.getDescuento());
		t.setImportePagado(req.getImportePagado());
		t.setEmpresa(empresa);

		// si quieres, descripción automática:
		t.setDescripcion(p.getNombre());

		return trabajoRepo.save(t);
	}

	@GetMapping("/{clienteId}/productos")
	public ResponseEntity<?> getProductosComprados(@PathVariable Long clienteId, HttpServletRequest request) {
		Cliente c = clienteRepo.findById(clienteId).orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

		// empresa por header o del cliente (igual que en POST)
		String empresaHeader = request.getHeader("X-Empresa");
		String empresa = (empresaHeader != null && !empresaHeader.isBlank()) ? empresaHeader.trim() : c.getEmpresa();

		if (empresa == null || empresa.isBlank()) {
			return ResponseEntity.badRequest().body("Empresa no determinada");
		}

		// 1) sacar trabajos del cliente (solo los que vienen de productos: productoId
		// != null)
		List<Trabajo> trabajos = trabajoRepo.findByClienteIdAndEmpresa(clienteId, empresa).stream()
				.filter(t -> t.getProductoId() != null).collect(Collectors.toList());

		if (trabajos.isEmpty()) {
			return ResponseEntity.ok(List.of());
		}

		// 2) sumar unidades por productoId
		Map<Long, Integer> unidadesPorProducto = new HashMap<>();
		for (Trabajo t : trabajos) {
			Long pid = t.getProductoId();
			int u = (t.getUnidades() != null) ? t.getUnidades() : 0;
			unidadesPorProducto.merge(pid, u, Integer::sum);
		}

		// 3) traer productos y construir DTO
		List<Long> ids = new ArrayList<>(unidadesPorProducto.keySet());
		List<Producto> productos = productoRepo.findAllById(ids);

		List<ClienteProductoCompradoDTO> res = productos.stream()
				.map(p -> new ClienteProductoCompradoDTO(p.getId(), p.getCodigo(), p.getNombre(),
						unidadesPorProducto.getOrDefault(p.getId(), 0), false, null))
				.sorted(Comparator.comparing(ClienteProductoCompradoDTO::getNombre, String.CASE_INSENSITIVE_ORDER))
				.collect(Collectors.toList());

		return ResponseEntity.ok(res);
	}

}

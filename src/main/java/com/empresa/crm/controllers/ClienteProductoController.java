package com.empresa.crm.controllers;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.empresa.crm.dto.AddProductoRequest;
import com.empresa.crm.dto.ClienteProductoAsignarDTO;
import com.empresa.crm.dto.ClienteProductoCompradoDTO;
import com.empresa.crm.entities.Cliente;
import com.empresa.crm.entities.ClienteProducto;
import com.empresa.crm.entities.Producto;
import com.empresa.crm.entities.Trabajo;
import com.empresa.crm.repositories.ClienteProductoRepository;
import com.empresa.crm.repositories.ClienteRepository;
import com.empresa.crm.repositories.ProductoRepository;
import com.empresa.crm.repositories.RutaRepository;
import com.empresa.crm.repositories.TrabajoRepository;
import com.empresa.crm.serviceImpl.ClienteProductoServiceImpl;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;

@RestController
@RequestMapping("/api/clientes")
@CrossOrigin(origins = "http://localhost:4200")
public class ClienteProductoController {

	private final com.empresa.crm.services.TrabajoService trabajoService;
	private final ClienteRepository clienteRepo;
	private final ProductoRepository productoRepo;
	private final TrabajoRepository trabajoRepo;
	private final ClienteProductoRepository clienteProductoRepo;
	private final ClienteProductoServiceImpl clienteProductoService;
	private final RutaRepository rutaRepository;

	public ClienteProductoController(com.empresa.crm.services.TrabajoService trabajoService,
			ClienteRepository clienteRepo, ProductoRepository productoRepo, TrabajoRepository trabajoRepo,
			ClienteProductoRepository clienteProductoRepo, ClienteProductoServiceImpl clienteProductoService,
			RutaRepository rutaRepository) {
		this.trabajoService = trabajoService;
		this.clienteRepo = clienteRepo;
		this.productoRepo = productoRepo;
		this.trabajoRepo = trabajoRepo;
		this.clienteProductoRepo = clienteProductoRepo;
		this.clienteProductoService = clienteProductoService;
		this.rutaRepository = rutaRepository;
	}

	@GetMapping("/{clienteId}/productos/pendientes")
	public ResponseEntity<?> getProductosPendientes(@PathVariable Long clienteId,
			@RequestParam(value = "excludeRutaId", required = false) Long excludeRutaId, HttpServletRequest request) {

		Cliente c = clienteRepo.findById(clienteId).orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

		String empresaHeader = request.getHeader("X-Empresa");
		String empresa = (empresaHeader != null && !empresaHeader.isBlank()) ? empresaHeader.trim() : c.getEmpresa();

		if (empresa == null || empresa.isBlank()) {
			return ResponseEntity.badRequest().body("Empresa no determinada");
		}

		List<Trabajo> trabajosPendientes = trabajoRepo.findByClienteIdAndEmpresa(clienteId, empresa).stream()
				.filter(t -> t.getProductoId() != null).filter(t -> !t.isEntregado()).collect(Collectors.toList());

		if (trabajosPendientes.isEmpty()) {
			return ResponseEntity.ok(List.of());
		}

		Map<Long, Integer> unidadesPorProducto = new HashMap<>();
		for (Trabajo t : trabajosPendientes) {
			Long pid = t.getProductoId();
			int u = safeInt(t.getUnidades());
			if (u <= 0) {
				u = 1;
			}
			unidadesPorProducto.merge(pid, u, Integer::sum);
		}

		List<Long> ids = new ArrayList<>(unidadesPorProducto.keySet());

		// ✅ Filtramos además por empresa para evitar mezclar productos
		List<Producto> productos = productoRepo.findAllById(ids).stream()
				.filter(p -> p.getEmpresa() != null && p.getEmpresa().equalsIgnoreCase(empresa))
				.collect(Collectors.toList());

		LocalDate fechaReferencia = LocalDate.now();

		List<ClienteProductoCompradoDTO> res = productos.stream().map(p -> {
			int totalPendienteTrabajo = unidadesPorProducto.getOrDefault(p.getId(), 0);

			int reservadoAbierto = safeInt(rutaRepository.sumReservadoClienteProductoAbiertoEnFechaExcluyendoRuta(
					empresa, fechaReferencia, clienteId, p.getId(), excludeRutaId));

			int pendienteReal = Math.max(totalPendienteTrabajo - reservadoAbierto, 0);

			return new ClienteProductoCompradoDTO(p.getId(), p.getCodigo(), p.getNombre(), pendienteReal, false, null);
		}).filter(dto -> dto.getCantidad() != null && dto.getCantidad() > 0)
				.sorted(Comparator.comparing(ClienteProductoCompradoDTO::getNombre, String.CASE_INSENSITIVE_ORDER))
				.collect(Collectors.toList());

		return ResponseEntity.ok(res);
	}

	@PostMapping("/{clienteId}/productos/{productoId}")
	@Transactional
	public Trabajo addProductoCliente(@PathVariable Long clienteId, @PathVariable Long productoId,
			@RequestBody(required = false) AddProductoRequest req,
			@RequestHeader(value = "X-Empresa", required = false) String empresaHeader) {

		if (req == null) {
			req = new AddProductoRequest();
		}

		Cliente cliente = clienteRepo.findById(clienteId)
				.orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

		String empresa = (empresaHeader != null && !empresaHeader.isBlank()) ? empresaHeader.trim()
				: cliente.getEmpresa();

		if (empresa == null || empresa.isBlank()) {
			throw new RuntimeException("Empresa no determinada");
		}

		int cantidad = req.getCantidad() != null ? req.getCantidad() : 1;
		if (cantidad <= 0) {
			cantidad = 1;
		}

		Producto p = productoRepo.findById(productoId).orElseThrow(() -> new RuntimeException("Producto no existe"));

		if (!p.getEmpresa().equalsIgnoreCase(empresa)) {
			throw new RuntimeException("Producto no pertenece a la empresa activa");
		}

		int stockActual = safeInt(p.getStock());
		if (cantidad > stockActual) {
			throw new RuntimeException("Stock insuficiente");
		}

		int updated = productoRepo.decrementStockIfAvailable(productoId, cantidad, empresa);
		if (updated == 0) {
			throw new RuntimeException("Stock insuficiente");
		}

		Trabajo t = new Trabajo();
		t.setCliente(cliente);
		t.setProductoId(productoId);
		t.setUnidades(cantidad);
		t.setDescuento(req.getDescuento() != null ? req.getDescuento() : 0.0);
		t.setImportePagado(req.getImportePagado() != null ? req.getImportePagado() : 0.0);
		t.setEmpresa(empresa);
		t.setDescripcion(p.getNombre());

		ClienteProductoAsignarDTO dto = new ClienteProductoAsignarDTO();
		dto.setClienteId(clienteId);
		dto.setProductoId(productoId);
		dto.setCantidadTotal(cantidad);

		clienteProductoService.asignarProductoACliente(dto);

		return trabajoService.save(t);
	}

	@PutMapping("/{clienteId}/productos/{productoId}/cantidad")
	@Transactional
	public ResponseEntity<?> actualizarCantidad(@PathVariable Long clienteId, @PathVariable Long productoId,
			@RequestParam int cantidad, @RequestHeader(value = "X-Empresa", required = false) String empresaHeader) {

		Cliente cliente = clienteRepo.findById(clienteId)
				.orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

		String empresa = (empresaHeader != null && !empresaHeader.isBlank()) ? empresaHeader.trim()
				: cliente.getEmpresa();

		if (empresa == null || empresa.isBlank()) {
			throw new RuntimeException("Empresa no determinada");
		}

		List<Trabajo> trabajos = trabajoRepo.findByEmpresaAndClienteIdAndProductoId(empresa, clienteId, productoId);

		Trabajo trabajo = trabajos.stream().filter(t -> !t.isEntregado()).findFirst()
				.orElseThrow(() -> new RuntimeException("No existe ese producto pendiente en el cliente"));

		int actual = safeInt(trabajo.getUnidades());
		if (actual <= 0) {
			actual = 1;
		}

		ClienteProducto cp = clienteProductoRepo.findByEmpresaAndClienteIdAndProductoId(empresa, clienteId, productoId)
				.orElse(null);

		if (cantidad == actual) {
			return ResponseEntity.ok(trabajo);
		}

		// AUMENTAR
		if (cantidad > actual) {
			int diff = cantidad - actual;

			Producto producto = productoRepo.findByIdAndEmpresa(productoId, empresa)
					.orElseThrow(() -> new RuntimeException("Producto no encontrado"));

			int stockActual = safeInt(producto.getStock());
			if (diff > stockActual) {
				throw new RuntimeException("Stock insuficiente");
			}

			int updated = productoRepo.decrementStockIfAvailable(productoId, diff, empresa);
			if (updated == 0) {
				throw new RuntimeException("Stock insuficiente");
			}

			trabajo.setUnidades(cantidad);
			trabajoRepo.save(trabajo);

			if (cp != null) {
				cp.setCantidadTotal(safeInt(cp.getCantidadTotal()) + diff);
				normalizarClienteProducto(cp);
				clienteProductoRepo.save(cp);
			}

			return ResponseEntity.ok(trabajo);
		}

		// DISMINUIR
		int diff = actual - cantidad;

		if (diff > 0) {
			productoRepo.incrementStockByEmpresa(productoId, diff, empresa);
		}

		if (cp != null) {
			int nuevoTotal = safeInt(cp.getCantidadTotal()) - diff;

			if (nuevoTotal <= 0) {
				clienteProductoRepo.delete(cp);
			} else {
				cp.setCantidadTotal(nuevoTotal);
				normalizarClienteProducto(cp);
				clienteProductoRepo.save(cp);
			}
		}

		if (cantidad <= 0) {
			trabajoRepo.delete(trabajo);
			return ResponseEntity.ok().build();
		}

		trabajo.setUnidades(cantidad);
		trabajoRepo.save(trabajo);

		return ResponseEntity.ok(trabajo);
	}

	@GetMapping("/{clienteId}/productos")
	public ResponseEntity<?> getProductosComprados(@PathVariable Long clienteId, HttpServletRequest request) {
		Cliente c = clienteRepo.findById(clienteId).orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

		String empresaHeader = request.getHeader("X-Empresa");
		String empresa = (empresaHeader != null && !empresaHeader.isBlank()) ? empresaHeader.trim() : c.getEmpresa();

		if (empresa == null || empresa.isBlank()) {
			return ResponseEntity.badRequest().body("Empresa no determinada");
		}

		List<Trabajo> trabajos = trabajoRepo.findByClienteIdAndEmpresa(clienteId, empresa).stream()
				.filter(t -> t.getProductoId() != null).collect(Collectors.toList());

		if (trabajos.isEmpty()) {
			return ResponseEntity.ok(List.of());
		}

		Map<Long, Integer> unidadesPorProducto = new HashMap<>();
		for (Trabajo t : trabajos) {
			Long pid = t.getProductoId();
			int u = safeInt(t.getUnidades());
			unidadesPorProducto.merge(pid, u, Integer::sum);
		}

		List<Long> ids = new ArrayList<>(unidadesPorProducto.keySet());
		List<Producto> productos = productoRepo.findAllById(ids);

		List<ClienteProductoCompradoDTO> res = productos.stream()
				.map(p -> new ClienteProductoCompradoDTO(p.getId(), p.getCodigo(), p.getNombre(),
						unidadesPorProducto.getOrDefault(p.getId(), 0), false, null))
				.sorted(Comparator.comparing(ClienteProductoCompradoDTO::getNombre, String.CASE_INSENSITIVE_ORDER))
				.collect(Collectors.toList());

		return ResponseEntity.ok(res);
	}

	@DeleteMapping("/{clienteId}/productos/{productoId}")
	public ResponseEntity<Void> deleteProductoCliente(@PathVariable Long clienteId, @PathVariable Long productoId,
			@RequestHeader(value = "X-Empresa", required = false) String empresaHeader) {

		Cliente c = clienteRepo.findById(clienteId).orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

		String empresa = (empresaHeader != null && !empresaHeader.isBlank()) ? empresaHeader.trim() : c.getEmpresa();

		if (empresa == null || empresa.isBlank()) {
			throw new RuntimeException("Empresa no determinada");
		}

		trabajoService.deleteProductoCliente(clienteId, productoId, empresa);
		clienteProductoService.eliminarAsignacion(clienteId, productoId);

		return ResponseEntity.noContent().build();
	}

	@PostMapping("/asignar")
	public ResponseEntity<ClienteProducto> asignar(@RequestBody ClienteProductoAsignarDTO dto) {
		return ResponseEntity.ok(clienteProductoService.asignarProductoACliente(dto));
	}

	@GetMapping("/cliente/{clienteId}")
	public ResponseEntity<?> listado(@PathVariable Long clienteId) {
		return ResponseEntity.ok(clienteProductoService.listarPorCliente(clienteId));
	}

	@DeleteMapping("/cliente/{clienteId}/producto/{productoId}")
	public ResponseEntity<Void> eliminar(@PathVariable Long clienteId, @PathVariable Long productoId) {
		clienteProductoService.eliminarAsignacion(clienteId, productoId);
		return ResponseEntity.noContent().build();
	}

	private int safeInt(Integer n) {
		return n == null ? 0 : n.intValue();
	}

	private void normalizarClienteProducto(ClienteProducto cp) {
		if (cp == null) {
			return;
		}

		int total = safeInt(cp.getCantidadTotal());
		int entregada = safeInt(cp.getCantidadEntregada());

		if (entregada < 0) {
			entregada = 0;
		}
		if (entregada > total) {
			entregada = total;
		}

		cp.setCantidadTotal(total);
		cp.setCantidadEntregada(entregada);

		boolean entregadoCompleto = total > 0 && entregada >= total;
		cp.setEntregado(entregadoCompleto);

		if (entregadoCompleto) {
			cp.setEstado("ENTREGADO");
		} else if (entregada > 0) {
			cp.setEstado("PARCIAL");
		} else {
			cp.setEstado("PENDIENTE");
		}
	}
}
package com.empresa.crm.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.empresa.crm.entities.Inventario;
import com.empresa.crm.entities.InventarioLinea;
import com.empresa.crm.repositories.InventarioRepository;

import com.empresa.crm.entities.Producto;
import com.empresa.crm.repositories.ProductoRepository;

@RestController
@RequestMapping("/api/inventarios")
@CrossOrigin(origins = "*")
public class InventarioController {

	@Autowired
	private InventarioRepository inventarioRepository;

	@Autowired
	private ProductoRepository productoRepository;

	@GetMapping
	public List<Inventario> listarPorEmpresa(@RequestHeader("X-Empresa") String empresa) {
		return inventarioRepository.findByEmpresaOrderByFechaDesc(empresa);
	}

	@GetMapping("/{id}")
	public Inventario buscarPorId(@PathVariable Long id) {
		return inventarioRepository.findById(id).orElse(null);
	}

	@PostMapping
	public Inventario crearInventario(@RequestHeader("X-Empresa") String empresa, @RequestBody Inventario inventario) {

		inventario.setEmpresa(empresa);

		if (inventario.getLineas() != null) {
			for (InventarioLinea linea : inventario.getLineas()) {
				linea.setInventario(inventario);
				linea.recalcular();
			}
		}

		inventario.recalcularTotales();

		return inventarioRepository.save(inventario);
	}

	@DeleteMapping("/{id}")
	public void eliminarInventario(@PathVariable Long id) {
		inventarioRepository.deleteById(id);
	}

	@GetMapping("/preparar")
	public Inventario prepararInventario(@RequestHeader("X-Empresa") String empresa) {

		List<Producto> productos = productoRepository.findByEmpresa(empresa);

		Inventario inventario = new Inventario();
		inventario.setEmpresa(empresa);
		inventario.setDescripcion("");
		inventario.setRealizadoPor("");

		for (Producto producto : productos) {

			InventarioLinea linea = new InventarioLinea();

			linea.setProductoId(producto.getId());
			linea.setReferencia(producto.getReferencia());
			linea.setGama(producto.getGama());
			linea.setMarca(producto.getMarca());
			linea.setModelo(producto.getModelo());
			linea.setFamilia(producto.getFamilia());
			linea.setSubfamilia(producto.getSubfamilia());
			linea.setDescripcion(producto.getDescripcion());

			linea.setStockSistema(producto.getUnidades());
			linea.setStockContado(producto.getUnidades());

			linea.setPrecioUnitario(producto.getPrecioSinIva());

			linea.recalcular();

			inventario.getLineas().add(linea);
		}

		inventario.recalcularTotales();

		return inventario;
	}
}
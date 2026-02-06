package com.empresa.crm.services;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.empresa.crm.entities.Cliente;
import com.empresa.crm.entities.ClienteProducto;
import com.empresa.crm.entities.Producto;
import com.empresa.crm.repositories.ClienteProductoRepository;
import com.empresa.crm.repositories.ClienteRepository;
import com.empresa.crm.repositories.ProductoRepository;

import jakarta.transaction.Transactional;

@Service
public class ClienteProductoService {
	
	private final ProductoRepository productoRepo;
	private final ClienteRepository clienteRepo;
	private final ClienteProductoRepository cpRepo;
	
	public ClienteProductoService(ProductoRepository productoRepo,
			ClienteRepository clienteRepo,
			ClienteProductoRepository cpRepo) {
		this.productoRepo = productoRepo;
		this.clienteRepo = clienteRepo;
		this.cpRepo = cpRepo;
	}
	
	@Transactional
	public ClienteProducto addProductoToCliente(Long clienteId, Long productoId) {
		Cliente cliente = clienteRepo.findById(productoId)
				.orElseThrow(() -> new RuntimeException("Cliente no ecxiste"));
		
		Producto producto = productoRepo.findById(productoId)
				.orElseThrow(() -> new RuntimeException("Producto no existe"));
		
		// (Opcional) Validar empresa
		// if (!cliente.getEmpresa().equals(producto.getEmpresa())) throw...
		
		int updated = productoRepo.decrementStockIfAvailable(productoId);
		if(updated == 0) {
			// No habia stock
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sin stock disponible");
		}
		
		//Creamos la asignacion
		ClienteProducto cp = new ClienteProducto();
		cp.setCliente(cliente);
		cp.setProducto(producto);
		return cpRepo.save(cp);
	}

}

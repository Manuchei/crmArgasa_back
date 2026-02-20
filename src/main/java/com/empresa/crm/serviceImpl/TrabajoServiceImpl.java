package com.empresa.crm.serviceImpl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.empresa.crm.entities.Trabajo;
import com.empresa.crm.repositories.ProductoRepository;
import com.empresa.crm.repositories.TrabajoRepository;
import com.empresa.crm.services.TrabajoService;

import jakarta.transaction.Transactional;

@Service
public class TrabajoServiceImpl implements TrabajoService {

	private final TrabajoRepository trabajoRepository;
	private final ProductoRepository productoRepo;

	public TrabajoServiceImpl(TrabajoRepository trabajoRepository, ProductoRepository productoRepo) {
		this.trabajoRepository = trabajoRepository;
		this.productoRepo = productoRepo;
	}

	@Override
	public List<Trabajo> findAll() {
		return trabajoRepository.findAll();
	}

	@Override
	public Trabajo findById(Long id) {
		return trabajoRepository.findById(id).orElse(null);
	}

	@Override
	public Trabajo save(Trabajo trabajo) {
		return trabajoRepository.save(trabajo);
	}

	@Override
	public void deleteById(Long id) {
		trabajoRepository.deleteById(id);
	}

	@Override
	public List<Trabajo> findByProveedor(Long proveedorId) {
		return trabajoRepository.findByProveedorId(proveedorId);
	}

	@Override
	public List<Trabajo> findByPagado(boolean pagado) {
		return trabajoRepository.findByPagado(pagado);
	}

	@Override
	public List<Trabajo> findByCliente(Long clienteId) {
		return trabajoRepository.findByCliente_Id(clienteId);
	}

	@Override
	@Transactional
	public void deleteProductoCliente(Long clienteId, Long productoId, String empresa) {

		Trabajo t = trabajoRepository.findByClienteIdAndProductoIdAndEntregadoFalse(clienteId, productoId).stream()
				.filter(x -> empresa.equalsIgnoreCase(x.getEmpresa())).findFirst()
				.orElseThrow(() -> new RuntimeException("No hay trabajo pendiente para borrar"));

		int cantidad = (t.getUnidades() != null && t.getUnidades() > 0) ? t.getUnidades() : 1;

		productoRepo.incrementStock(productoId, cantidad);
		trabajoRepository.delete(t);
	}
}
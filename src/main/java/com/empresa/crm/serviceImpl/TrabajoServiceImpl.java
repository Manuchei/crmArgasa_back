package com.empresa.crm.serviceImpl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.empresa.crm.entities.Trabajo;
import com.empresa.crm.repositories.TrabajoRepository;
import com.empresa.crm.services.TrabajoService;

@Service
public class TrabajoServiceImpl implements TrabajoService {

	private final TrabajoRepository trabajoRepository;

	public TrabajoServiceImpl(TrabajoRepository trabajoRepository) {
		this.trabajoRepository = trabajoRepository;
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
}

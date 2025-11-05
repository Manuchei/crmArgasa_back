package com.empresa.crm.services;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.empresa.crm.entities.Cliente;
import com.empresa.crm.entities.FacturaCliente;
import com.empresa.crm.entities.ServicioCliente;
import com.empresa.crm.repositories.ClienteRepository;
import com.empresa.crm.repositories.FacturaClienteRepository;
import com.empresa.crm.repositories.ServicioClienteRepository;

@Service
public class FacturaClienteServiceImpl implements FacturaClienteService {

	private final FacturaClienteRepository facturaRepo;
	private final ClienteRepository clienteRepo;
	private final ServicioClienteRepository servicioRepo;

	public FacturaClienteServiceImpl(FacturaClienteRepository facturaRepo, ClienteRepository clienteRepo,
			ServicioClienteRepository servicioRepo) {
		this.facturaRepo = facturaRepo;
		this.clienteRepo = clienteRepo;
		this.servicioRepo = servicioRepo;
	}

	@Override
	public List<FacturaCliente> findAll() {
		return facturaRepo.findAll();
	}

	@Override
	public FacturaCliente findById(Long id) {
		return facturaRepo.findById(id).orElse(null);
	}

	@Override
	public FacturaCliente generarFactura(Long clienteId, String empresa) {
		Cliente cliente = clienteRepo.findById(clienteId).orElse(null);
		if (cliente == null)
			return null;

		// obtener servicios pendientes sin factura
		List<ServicioCliente> serviciosPendientes = servicioRepo.findByClienteId(clienteId).stream()
				.filter(s -> !s.isPagado() && s.getFactura() == null).collect(Collectors.toList());

		if (serviciosPendientes.isEmpty())
			return null;

		double total = serviciosPendientes.stream().mapToDouble(ServicioCliente::getImporte).sum();

		FacturaCliente factura = new FacturaCliente();
		factura.setCliente(cliente);
		factura.setEmpresa(empresa);
		factura.setFechaEmision(LocalDate.now());
		factura.setPagada(false);
		factura.setTotalImporte(total);
		factura = facturaRepo.save(factura);

		// vincular servicios con la factura
		for (ServicioCliente s : serviciosPendientes) {
			s.setFactura(factura);
			servicioRepo.save(s);
		}

		return factura;
	}

	@Override
	public FacturaCliente marcarComoPagada(Long facturaId) {
		FacturaCliente factura = facturaRepo.findById(facturaId).orElse(null);
		if (factura == null)
			return null;

		factura.setPagada(true);
		facturaRepo.save(factura);

		// marcar servicios como pagados
		if (factura.getServicios() != null) {
			for (ServicioCliente s : factura.getServicios()) {
				s.setPagado(true);
				servicioRepo.save(s);
			}
		}

		return factura;
	}

	@Override
	public List<FacturaCliente> findByEmpresa(String empresa) {
		return facturaRepo.findByEmpresa(empresa);
	}

	@Override
	public List<FacturaCliente> findByCliente(Long clienteId) {
		return facturaRepo.findAll().stream().filter(f -> f.getCliente().getId().equals(clienteId))
				.collect(Collectors.toList());
	}
}

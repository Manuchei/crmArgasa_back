package com.empresa.crm.serviceImpl;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.empresa.crm.entities.FacturaProveedor;
import com.empresa.crm.entities.Proveedor;
import com.empresa.crm.entities.Trabajo;
import com.empresa.crm.repositories.FacturaProveedorRepository;
import com.empresa.crm.repositories.ProveedorRepository;
import com.empresa.crm.repositories.TrabajoRepository;
import com.empresa.crm.services.FacturaProveedorService;

@Service
public class FacturaProveedorServiceImpl implements FacturaProveedorService {

	private final FacturaProveedorRepository facturaRepo;
	private final ProveedorRepository proveedorRepo;
	private final TrabajoRepository trabajoRepo;

	public FacturaProveedorServiceImpl(FacturaProveedorRepository facturaRepo, ProveedorRepository proveedorRepo,
			TrabajoRepository trabajoRepo) {
		this.facturaRepo = facturaRepo;
		this.proveedorRepo = proveedorRepo;
		this.trabajoRepo = trabajoRepo;
	}

	@Override
	public List<FacturaProveedor> findAll() {
		return facturaRepo.findAll();
	}

	@Override
	public FacturaProveedor findById(Long id) {
		return facturaRepo.findById(id).orElse(null);
	}

	@Override
	public FacturaProveedor generarFactura(Long proveedorId, String empresa) {
		Proveedor proveedor = proveedorRepo.findById(proveedorId).orElse(null);
		if (proveedor == null)
			return null;

		// obtener trabajos pendientes sin factura
		List<Trabajo> trabajosPendientes = trabajoRepo.findByProveedorId(proveedorId).stream()
				.filter(t -> !t.isPagado() && t.getFactura() == null).collect(Collectors.toList());

		if (trabajosPendientes.isEmpty())
			return null;

		double total = trabajosPendientes.stream().mapToDouble(Trabajo::getImporte).sum();

		FacturaProveedor factura = new FacturaProveedor();
		factura.setProveedor(proveedor);
		factura.setEmpresa(empresa);
		factura.setFechaEmision(LocalDate.now());
		factura.setPagada(false);
		factura.setTotalImporte(total);
		factura = facturaRepo.save(factura);

		// vincular trabajos con la factura
		for (Trabajo t : trabajosPendientes) {
			t.setFactura(factura);
			trabajoRepo.save(t);
		}

		return factura;
	}

	@Override
	public FacturaProveedor marcarComoPagada(Long facturaId) {
		FacturaProveedor factura = facturaRepo.findById(facturaId).orElse(null);
		if (factura == null)
			return null;

		factura.setPagada(true);
		facturaRepo.save(factura);

		// marcar los trabajos de esa factura como pagados
		if (factura.getTrabajos() != null) {
			for (Trabajo t : factura.getTrabajos()) {
				t.setPagado(true);
				trabajoRepo.save(t);
			}
		}

		return factura;
	}

	@Override
	public List<FacturaProveedor> findByEmpresa(String empresa) {
		return facturaRepo.findByEmpresa(empresa);
	}

	@Override
	public List<FacturaProveedor> findByProveedor(Long proveedorId) {
		return facturaRepo.findAll().stream().filter(f -> f.getProveedor().getId().equals(proveedorId))
				.collect(Collectors.toList());
	}
}

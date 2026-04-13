package com.empresa.crm.serviceImpl;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.empresa.crm.entities.FacturaProveedor;
import com.empresa.crm.entities.Proveedor;
import com.empresa.crm.entities.Trabajo;
import com.empresa.crm.repositories.FacturaProveedorRepository;
import com.empresa.crm.repositories.ProveedorRepository;
import com.empresa.crm.repositories.TrabajoRepository;
import com.empresa.crm.services.FacturaProveedorService;
import com.empresa.crm.tenant.TenantContext;

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
		String empresa = TenantContext.get();
		return facturaRepo.findByEmpresa(empresa);
	}

	@Override
	public FacturaProveedor findById(Long id) {
		String empresa = TenantContext.get();
		return facturaRepo.findByIdAndEmpresa(id, empresa).orElse(null);
	}

	@Override
	public FacturaProveedor generarFactura(Long proveedorId, String empresa) {
		String tenant = TenantContext.get();

		Proveedor proveedor = proveedorRepo.findByIdAndEmpresa(proveedorId, tenant).orElse(null);
		if (proveedor == null) {
			return null;
		}

		List<Trabajo> trabajosPendientes = trabajoRepo.findByProveedorId(proveedorId).stream()
				.filter(t -> !t.isPagado() && t.getFactura() == null).collect(Collectors.toList());

		if (trabajosPendientes.isEmpty()) {
			return null;
		}

		double total = trabajosPendientes.stream().mapToDouble(t -> t.getImporte() != null ? t.getImporte() : 0.0)
				.sum();

		FacturaProveedor factura = new FacturaProveedor();
		factura.setProveedor(proveedor);
		factura.setEmpresa(tenant);
		factura.setFechaEmision(LocalDate.now());
		factura.setPagada(false);
		factura.setTotalImporte(total);
		factura.setNumeroInterno(generarNumeroInterno(tenant));
		factura.setNumeroFacturaProveedor(null);

		factura = facturaRepo.save(factura);

		for (Trabajo trabajo : trabajosPendientes) {
			trabajo.setFactura(factura);
			trabajoRepo.save(trabajo);
		}

		return factura;
	}

	@Override
	public FacturaProveedor marcarComoPagada(Long facturaId) {
		String empresa = TenantContext.get();

		FacturaProveedor factura = facturaRepo.findByIdAndEmpresa(facturaId, empresa).orElse(null);
		if (factura == null) {
			return null;
		}

		factura.setPagada(true);
		facturaRepo.save(factura);

		if (factura.getTrabajos() != null) {
			for (Trabajo trabajo : factura.getTrabajos()) {
				trabajo.setPagado(true);
				trabajoRepo.save(trabajo);
			}
		}

		return factura;
	}

	@Override
	public FacturaProveedor actualizarNumeroFacturaProveedor(Long facturaId, String numeroFacturaProveedor) {
		String empresa = TenantContext.get();

		FacturaProveedor factura = facturaRepo.findByIdAndEmpresa(facturaId, empresa).orElse(null);
		if (factura == null) {
			return null;
		}

		factura.setNumeroFacturaProveedor(numeroFacturaProveedor);
		return facturaRepo.save(factura);
	}

	@Override
	public List<FacturaProveedor> findByEmpresa(String empresa) {
		return facturaRepo.findByEmpresa(TenantContext.get());
	}

	@Override
	public List<FacturaProveedor> findByProveedor(Long proveedorId) {
		String empresa = TenantContext.get();
		return facturaRepo.findByProveedorIdAndEmpresa(proveedorId, empresa);
	}

	private String generarNumeroInterno(String empresa) {
		int anio = LocalDate.now().getYear();
		String prefijoEmpresa = normalizarEmpresa(empresa);

		Optional<FacturaProveedor> ultimaFacturaOpt = facturaRepo.findTopByEmpresaOrderByIdDesc(empresa);

		int siguienteNumero = 1;

		if (ultimaFacturaOpt.isPresent()) {
			String ultimoNumeroInterno = ultimaFacturaOpt.get().getNumeroInterno();

			if (ultimoNumeroInterno != null && !ultimoNumeroInterno.isBlank()) {
				String[] partes = ultimoNumeroInterno.split("-");
				if (partes.length == 4) {
					try {
						int ultimoCorrelativo = Integer.parseInt(partes[3]);
						int ultimoAnio = Integer.parseInt(partes[2]);

						if (ultimoAnio == anio) {
							siguienteNumero = ultimoCorrelativo + 1;
						}
					} catch (NumberFormatException e) {
						siguienteNumero = 1;
					}
				}
			}
		}

		return String.format("FP-%s-%d-%04d", prefijoEmpresa, anio, siguienteNumero);
	}

	private String normalizarEmpresa(String empresa) {
		if (empresa == null || empresa.isBlank()) {
			return "GEN";
		}

		String empresaNormalizada = empresa.trim().toUpperCase();

		if (empresaNormalizada.equals("ARGASA")) {
			return "ARG";
		}

		if (empresaNormalizada.equals("LUGA") || empresaNormalizada.equals("ELECTROLUGA")) {
			return "LUG";
		}

		return empresaNormalizada.length() >= 3 ? empresaNormalizada.substring(0, 3) : empresaNormalizada;
	}
}
package com.empresa.crm.serviceImpl;

import java.time.YearMonth;
import java.util.List;

import org.springframework.stereotype.Service;

import com.empresa.crm.dto.ResumenMensualDTO;
import com.empresa.crm.entities.FacturaCliente;
import com.empresa.crm.entities.FacturaProveedor;
import com.empresa.crm.repositories.FacturaClienteRepository;
import com.empresa.crm.repositories.FacturaProveedorRepository;
import com.empresa.crm.services.InformeMensualService;

@Service
public class InformeMensualServiceImpl implements InformeMensualService {

	private final FacturaClienteRepository facturaClienteRepo;
	private final FacturaProveedorRepository facturaProveedorRepo;

	public InformeMensualServiceImpl(FacturaClienteRepository facturaClienteRepo,
			FacturaProveedorRepository facturaProveedorRepo) {
		this.facturaClienteRepo = facturaClienteRepo;
		this.facturaProveedorRepo = facturaProveedorRepo;
	}

	@Override
	public ResumenMensualDTO generarResumen(String empresa, int year, int month) {
		YearMonth yearMonth = YearMonth.of(year, month);

		// --- FACTURAS CLIENTES ---
		List<FacturaCliente> facturasClientes = facturaClienteRepo.findByEmpresa(empresa);
		facturasClientes = facturasClientes.stream()
				.filter(f -> f.getFechaEmision() != null && YearMonth.from(f.getFechaEmision()).equals(yearMonth))
				.toList();

		double totalClientes = facturasClientes.stream().mapToDouble(FacturaCliente::getTotalImporte).sum();
		double totalClientesPagado = facturasClientes.stream().filter(FacturaCliente::isPagada)
				.mapToDouble(FacturaCliente::getTotalImporte).sum();
		double totalClientesPendiente = totalClientes - totalClientesPagado;

		// --- FACTURAS PROVEEDORES ---
		List<FacturaProveedor> facturasProveedores = facturaProveedorRepo.findByEmpresa(empresa);
		facturasProveedores = facturasProveedores.stream()
				.filter(f -> f.getFechaEmision() != null && YearMonth.from(f.getFechaEmision()).equals(yearMonth))
				.toList();

		double totalProveedores = facturasProveedores.stream().mapToDouble(FacturaProveedor::getTotalImporte).sum();
		double totalProveedoresPagado = facturasProveedores.stream().filter(FacturaProveedor::isPagada)
				.mapToDouble(FacturaProveedor::getTotalImporte).sum();
		double totalProveedoresPendiente = totalProveedores - totalProveedoresPagado;

		return new ResumenMensualDTO(/*empresa,*/ yearMonth.toString(), facturasClientes.size(), totalClientes,
				totalClientesPagado, totalClientesPendiente, facturasProveedores.size(), totalProveedores,
				totalProveedoresPagado, totalProveedoresPendiente);
	}
}

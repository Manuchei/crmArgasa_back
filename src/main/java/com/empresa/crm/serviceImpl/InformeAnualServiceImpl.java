package com.empresa.crm.serviceImpl;

import com.empresa.crm.dto.ResumenAnualDTO;
import com.empresa.crm.dto.ResumenMensualSimpleDTO;
import com.empresa.crm.entities.FacturaCliente;
import com.empresa.crm.entities.FacturaProveedor;
import com.empresa.crm.repositories.FacturaClienteRepository;
import com.empresa.crm.repositories.FacturaProveedorRepository;
import com.empresa.crm.services.InformeAnualService;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
public class InformeAnualServiceImpl implements InformeAnualService {

	private final FacturaClienteRepository facturaClienteRepo;
	private final FacturaProveedorRepository facturaProveedorRepo;

	public InformeAnualServiceImpl(FacturaClienteRepository facturaClienteRepo,
			FacturaProveedorRepository facturaProveedorRepo) {
		this.facturaClienteRepo = facturaClienteRepo;
		this.facturaProveedorRepo = facturaProveedorRepo;
	}

	@Override
	public ResumenAnualDTO generarResumenAnual(String empresa, int year) {
		List<ResumenMensualSimpleDTO> listaMensual = new ArrayList<>();
		double beneficioTotal = 0.0;

		for (int month = 1; month <= 12; month++) {
			final int mesActual = month; // ✅ ESTA LÍNEA SOLUCIONA EL ERROR

			YearMonth yearMonth = YearMonth.of(year, mesActual);

			// --- FACTURAS CLIENTES ---
			List<FacturaCliente> facturasClientes = facturaClienteRepo.findByEmpresa(empresa).stream()
					.filter(f -> f.getFechaEmision() != null && f.getFechaEmision().getYear() == year
							&& f.getFechaEmision().getMonthValue() == mesActual)
					.toList();

			double totalClientes = facturasClientes.stream().mapToDouble(FacturaCliente::getTotalImporte).sum();

			// --- FACTURAS PROVEEDORES ---
			List<FacturaProveedor> facturasProveedores = facturaProveedorRepo.findByEmpresa(empresa).stream()
					.filter(f -> f.getFechaEmision() != null && f.getFechaEmision().getYear() == year
							&& f.getFechaEmision().getMonthValue() == mesActual)
					.toList();

			double totalProveedores = facturasProveedores.stream().mapToDouble(FacturaProveedor::getTotalImporte).sum();

			double beneficio = totalClientes - totalProveedores;
			beneficioTotal += beneficio;

			listaMensual.add(new ResumenMensualSimpleDTO(String.format("%02d", mesActual), totalClientes,
					totalProveedores, beneficio));
		}

		return new ResumenAnualDTO(/*empresa,*/ year, beneficioTotal, listaMensual);

	}
}

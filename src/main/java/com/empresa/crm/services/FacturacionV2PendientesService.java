package com.empresa.crm.services;

import org.springframework.stereotype.Service;

import com.empresa.crm.dto.facturacionv2.LineaAlbaranPendienteDTO;
import com.empresa.crm.dto.facturacionv2.PendientesFacturacionDTO;
import com.empresa.crm.dto.facturacionv2.ServicioPendienteDTO;
import com.empresa.crm.repositories.LineaAlbaranClienteRepository;
import com.empresa.crm.repositories.ServicioClienteRepository;
import com.empresa.crm.tenant.TenantContext;

@Service
public class FacturacionV2PendientesService {

	private final ServicioClienteRepository servicioRepo;
	private final LineaAlbaranClienteRepository lineaRepo;

	public FacturacionV2PendientesService(ServicioClienteRepository servicioRepo,
			LineaAlbaranClienteRepository lineaRepo) {

		this.servicioRepo = servicioRepo;
		this.lineaRepo = lineaRepo;
	}

	public PendientesFacturacionDTO obtenerPendientes(Long clienteId) {
		String empresa = TenantContext.get();
		if (empresa == null || empresa.isBlank()) {
			throw new RuntimeException("Empresa no seleccionada (TenantContext vacío).");
		}
		var servicios = servicioRepo.findByClienteIdAndEmpresaAndFacturaV2IdIsNull(clienteId, empresa).stream()
				.map(s -> new ServicioPendienteDTO(s.getId(), s.getDescripcion(), s.getFecha(), s.getImporte()))
				.toList();

		var lineas = lineaRepo.pendientesPorCliente(clienteId, empresa).stream()
				.map(l -> new LineaAlbaranPendienteDTO(l.getId(),
						l.getAlbaran() != null ? l.getAlbaran().getId() : null, l.getDescripcion(), l.getUnidades(),
						l.getPrecio(), l.getDtoPct(), l.getTotalLinea()))
				.toList();

		return new PendientesFacturacionDTO(servicios, lineas);

	}

}

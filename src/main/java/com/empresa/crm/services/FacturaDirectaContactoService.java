package com.empresa.crm.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.empresa.crm.entities.Cliente;
import com.empresa.crm.repositories.ClienteRepository;
import com.empresa.crm.tenant.TenantContext;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

@Service
public class FacturaDirectaContactoService {

	private final ClienteRepository clienteRepo;
	private final FacturaDirectaService facturaDirecta;
	private final EntityManager entityManager;

	public FacturaDirectaContactoService(ClienteRepository clienteRepo, FacturaDirectaService facturaDirecta,
			EntityManager entityManager) {

		this.clienteRepo = clienteRepo;
		this.facturaDirecta = facturaDirecta;
		this.entityManager = entityManager;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public String obtenerContactoPrueba(Long clienteId) {
		String empresa = TenantContext.get();

		if (empresa == null || empresa.isBlank()) {
			throw new IllegalStateException("Empresa no seleccionada");
		}

		Cliente cliente = clienteRepo.findByIdAndEmpresa(clienteId, empresa)
				.orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado en la empresa seleccionada"));

		// Recarga y bloquea este cliente durante la operación.
		entityManager.refresh(cliente, LockModeType.PESSIMISTIC_WRITE);

		String company = facturaDirecta.obtenerCompanyIdPrueba(cliente.getEmpresa());

		String companyGuardada = cliente.getFacturaDirectaCompanyId();

		if (companyGuardada != null && !companyGuardada.isBlank() && !company.equals(companyGuardada)) {
			throw new IllegalStateException("El cliente está vinculado a otra empresa de FacturaDirecta");
		}

		String contactoId = facturaDirecta.obtenerOCrearContactoPrueba(cliente);

		cliente.setFacturaDirectaCompanyId(company);
		cliente.setFacturaDirectaContactId(contactoId);

		clienteRepo.saveAndFlush(cliente);

		return contactoId;
	}
}
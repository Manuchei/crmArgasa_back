package com.empresa.crm.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.empresa.crm.entities.facturacionV2.FacturaV2;
import com.empresa.crm.repositories.facturacionV2.FacturaV2Repository;
import com.empresa.crm.services.FacturaDirectaService;
import com.empresa.crm.tenant.TenantContext;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

@Service
public class FacturaDirectaEstadoService {

	private final FacturaV2Repository facturaRepo;
	private final FacturaDirectaService facturaDirecta;
	private final EntityManager entityManager;

	public FacturaDirectaEstadoService(FacturaV2Repository facturaRepo, FacturaDirectaService facturaDirecta,
			EntityManager entityManager) {

		this.facturaRepo = facturaRepo;
		this.facturaDirecta = facturaDirecta;
		this.entityManager = entityManager;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public FacturaV2 cargarFactura(Long facturaId) {
		FacturaV2 factura = cargar(facturaId);

		// Cargar los datos necesarios antes de cerrar la transacción.
		factura.getCliente().getNombreApellidos();
		factura.getLineas().forEach(linea -> linea.getDescripcion());

		return factura;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void reservarCreacion(Long facturaId) {
		FacturaV2 factura = cargar(facturaId);
		entityManager.refresh(factura, LockModeType.PESSIMISTIC_WRITE);

		String company = facturaDirecta.obtenerCompanyIdPrueba(factura.getEmpresa());

		if (!"BORRADOR".equalsIgnoreCase(factura.getEstado())) {
			throw new IllegalStateException("La factura no está en BORRADOR");
		}

		if (tieneTexto(factura.getFacturaDirectaId())) {
			throw new IllegalStateException("La factura ya está vinculada a FacturaDirecta");
		}

		if (tieneTexto(factura.getVerifactuEstado())) {
			throw new IllegalStateException(
					"Existe un intento previo. Debe recuperarse " + "antes de crear otra factura");
		}

		factura.setFacturaDirectaCompanyId(company);
		factura.setVerifactuEstado("CREACION_EN_CURSO");

		facturaRepo.saveAndFlush(factura);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void guardarVinculo(Long facturaId, String remotoId) {
		if (remotoId == null || !remotoId.startsWith("inv_")) {
			throw new IllegalArgumentException("ID remoto no válido");
		}

		FacturaV2 factura = cargar(facturaId);
		entityManager.refresh(factura, LockModeType.PESSIMISTIC_WRITE);

		String company = facturaDirecta.obtenerCompanyIdPrueba(factura.getEmpresa());

		if (tieneTexto(factura.getFacturaDirectaCompanyId()) && !company.equals(factura.getFacturaDirectaCompanyId())) {
			throw new IllegalStateException("La factura está vinculada a otra empresa remota");
		}

		if (tieneTexto(factura.getFacturaDirectaId()) && !remotoId.equals(factura.getFacturaDirectaId())) {
			throw new IllegalStateException("La factura ya tiene otro ID remoto");
		}

		factura.setFacturaDirectaCompanyId(company);
		factura.setFacturaDirectaId(remotoId);

		// No sobrescribir el estado de una factura ya vinculada.
		if (!tieneTexto(factura.getVerifactuEstado()) || "CREACION_EN_CURSO".equals(factura.getVerifactuEstado())) {
			factura.setVerifactuEstado("BORRADOR_REMOTO");
		}

		facturaRepo.saveAndFlush(factura);
	}

	private FacturaV2 cargar(Long facturaId) {
		String empresa = TenantContext.get();

		if (empresa == null || empresa.isBlank()) {
			throw new IllegalStateException("Empresa no seleccionada");
		}

		return facturaRepo.findByIdAndEmpresa(facturaId, empresa)
				.orElseThrow(() -> new IllegalArgumentException("Factura no encontrada en la empresa seleccionada"));
	}

	private boolean tieneTexto(String valor) {
		return valor != null && !valor.isBlank();
	}
}
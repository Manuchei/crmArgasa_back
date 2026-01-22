package com.empresa.crm.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.empresa.crm.dto.PagoTrabajoRequest;
import com.empresa.crm.entities.PagoTrabajo;
import com.empresa.crm.entities.Trabajo;
import com.empresa.crm.repositories.PagoTrabajoRepository;
import com.empresa.crm.repositories.TrabajoRepository;

@Service
public class PagoTrabajoService {

	private final PagoTrabajoRepository pagoRepo;
	private final TrabajoRepository trabajoRepo;

	public PagoTrabajoService(PagoTrabajoRepository pagoRepo, TrabajoRepository trabajoRepo) {
		this.pagoRepo = pagoRepo;
		this.trabajoRepo = trabajoRepo;
	}

	public List<PagoTrabajo> listarPorCliente(Long clienteId) {
		return pagoRepo.findByClienteIdOrderByFechaDesc(clienteId);
	}

	public List<PagoTrabajo> listarPorTrabajo(Long trabajoId) {
		return pagoRepo.findByTrabajoIdOrderByFechaDesc(trabajoId);
	}

	@Transactional
	public PagoTrabajo registrarPago(Long trabajoId, PagoTrabajoRequest req) {

		Trabajo t = trabajoRepo.findById(trabajoId).orElse(null);
		if (t == null)
			throw new RuntimeException("Trabajo no encontrado");

		double importePago = safe(req.getImporte());
		if (importePago <= 0)
			throw new RuntimeException("Importe inválido");

		// Guardamos el pago (histórico)
		PagoTrabajo p = new PagoTrabajo();
		p.setTrabajo(t);
		p.setCliente(t.getCliente());
		p.setImporte(importePago);
		p.setMetodo(req.getMetodo());
		p.setObservaciones(req.getObservaciones());

		pagoRepo.save(p);

		// Actualizamos el trabajo
		double pagadoActual = safe(t.getImportePagado());
		t.setImportePagado(pagadoActual + importePago);

		double total = safe(t.getImporte());
		t.setPagado(t.getImportePagado() >= total && total > 0);

		trabajoRepo.save(t);

		return p;
	}

	private double safe(Double v) {
		return v != null ? v : 0.0;
	}
}

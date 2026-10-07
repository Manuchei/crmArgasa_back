package com.empresa.crm.services;

import com.empresa.crm.entities.RegistroActividad;
import com.empresa.crm.repositories.RegistroActividadRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class RegistroActividadService {

	private final RegistroActividadRepository repository;

	public RegistroActividadService(RegistroActividadRepository repository) {
		this.repository = repository;
	}

	@Transactional
	public void registrar(String actor, String accion, String detalle, Integer estado, String empresa) {

		RegistroActividad registro = new RegistroActividad();

		registro.setFecha(Instant.now());
		registro.setActor(actor);
		registro.setAccion(accion);
		registro.setEstado(estado);
		registro.setEmpresa(empresa);

		// Mantener el detalle dentro del tamaño de la columna.
		registro.setDetalle(detalle == null ? null : detalle.substring(0, Math.min(detalle.length(), 2000)));

		repository.save(registro);
	}
}
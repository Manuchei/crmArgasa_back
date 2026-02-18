package com.empresa.crm.services;

import java.util.List;

import com.empresa.crm.entities.RutaLinea;

public interface RutaLineaService {

	List<RutaLinea> findByRuta(Long rutaId);

	void confirmarEntrega(Long rutaLineaId);

}

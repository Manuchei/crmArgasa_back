package com.empresa.crm.services;

import java.time.LocalDate;
import java.util.List;

import com.empresa.crm.dto.RutaDiaRequestDTO;
import com.empresa.crm.entities.Ruta;

public interface RutaService {

	List<Ruta> findAll();

	Ruta findById(Long id);

	Ruta save(Ruta ruta);

	void deleteById(Long id);

	List<Ruta> findByEstado(String estado);

	List<Ruta> findByNombreTransportista(String nombre);

	List<Ruta> findByFecha(LocalDate fecha);

	Ruta cerrarRuta(Long id);
	
	List <Ruta> crearRutasDeUnDia(RutaDiaRequestDTO request);
}

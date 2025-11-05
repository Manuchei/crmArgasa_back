package com.empresa.crm.serviceImpl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.empresa.crm.entities.Ruta;
import com.empresa.crm.repositories.RutaRepository;
import com.empresa.crm.services.RutaService;

@Service
public class RutaServiceImpl implements RutaService {

	private final RutaRepository rutaRepository;

	public RutaServiceImpl(RutaRepository rutaRepository) {
		this.rutaRepository = rutaRepository;
	}

	@Override
	public List<Ruta> findAll() {
		return rutaRepository.findAll();
	}

	@Override
	public Ruta findById(Long id) {
		return rutaRepository.findById(id).orElse(null);
	}

	@Override
	public Ruta save(Ruta ruta) {
		return rutaRepository.save(ruta);
	}

	@Override
	public void deleteById(Long id) {
		rutaRepository.deleteById(id);
	}

	@Override
	public List<Ruta> findByEstado(String estado) {
		return rutaRepository.findByEstado(estado);
	}

	@Override
	public List<Ruta> findByNombreTransportista(String nombre) {
		return rutaRepository.findByNombreTransportistaContainingIgnoreCase(nombre);
	}

	@Override
	public List<Ruta> findByFecha(LocalDate fecha) {
		return rutaRepository.findByFecha(fecha);
	}

	@Override
	public Ruta cerrarRuta(Long id) {
		Ruta ruta = findById(id);
		if (ruta != null && !"cerrada".equalsIgnoreCase(ruta.getEstado())) {
			ruta.setEstado("cerrada");
			rutaRepository.save(ruta);
		}
		return ruta;
	}
}

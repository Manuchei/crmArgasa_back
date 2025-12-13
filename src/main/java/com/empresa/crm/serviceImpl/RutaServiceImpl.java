package com.empresa.crm.serviceImpl;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.empresa.crm.entities.Ruta;
import com.empresa.crm.repositories.RutaRepository;
import com.empresa.crm.scheduler.RutaScheduler;
import com.empresa.crm.services.RutaService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RutaServiceImpl implements RutaService {

	private final RutaRepository rutaRepository;
	private final RutaScheduler rutaScheduler;

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

		LocalDate hoy = LocalDate.now();
		LocalTime ahora = LocalTime.now();

		boolean esNueva = (ruta.getId() == null);

		Ruta guardada = rutaRepository.save(ruta);

		boolean rutaEsHoy = ruta.getFecha() != null && ruta.getFecha().isEqual(hoy);
		boolean despuesDeLas8 = ahora.isAfter(LocalTime.of(8, 0));

		// 🔄 Si se crea o modifica ruta de hoy después de las 08:00 → enviar
		// actualización
		if (rutaEsHoy && despuesDeLas8) {
			rutaScheduler.enviarActualizacionHoy(ruta.getNombreTransportista());
		}

		return guardada;
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

			// Si es hoy y son más de las 8 → actualizamos correo
			LocalDate hoy = LocalDate.now();
			LocalTime ahora = LocalTime.now();

			if (ruta.getFecha() != null && ruta.getFecha().isEqual(hoy) && ahora.isAfter(LocalTime.of(8, 0))) {

				rutaScheduler.enviarActualizacionHoy(ruta.getNombreTransportista());
			}
		}

		return ruta;
	}
}

package com.empresa.crm.serviceImpl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.empresa.crm.entities.Ruta;
import com.empresa.crm.repositories.RutaRepository;
import com.empresa.crm.services.EmailService;
import com.empresa.crm.services.RutaService;

@Service
public class RutaServiceImpl implements RutaService {

	private final RutaRepository rutaRepository;
	private final EmailService emailService;

	public RutaServiceImpl(RutaRepository rutaRepository, EmailService emailService) {
		this.rutaRepository = rutaRepository;
		this.emailService = emailService;
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

		boolean esNueva = (ruta.getId() == null);

		Ruta rutaGuardada = rutaRepository.save(ruta);

		// CORREO NUEVA RUTA
		if (esNueva) {
			String asunto = "Nueva ruta asignada";
			String msg = "Hola " + ruta.getNombreTransportista() + "\n\nSe te ha asignado una nueva ruta."
					+ "\nOrigen: " + ruta.getOrigen() + "\nDestino: " + ruta.getDestino() + "\nFecha: "
					+ ruta.getFecha() + "\nObservaciones: " + ruta.getObservaciones();

			emailService.enviarCorreo(
				    ruta.getEmailTransportista(),   // ← AHORA USAMOS EL CORREO REAL
				    asunto,
				    msg
				);
		}
		// CORREO ACTUALIZACIÓN
		else {
			String asunto = "Actualización de ruta";
			String msg = "Hola " + ruta.getNombreTransportista() + "\n\nLa ruta asignada ha sido modificada."
					+ "\nOrigen: " + ruta.getOrigen() + "\nDestino: " + ruta.getDestino() + "\nNueva fecha: "
					+ ruta.getFecha() + "\nEstado: " + ruta.getEstado() + "\nObservaciones: " + ruta.getObservaciones();

			emailService.enviarCorreo(
				    ruta.getEmailTransportista(),   // ← AHORA USAMOS EL CORREO REAL
				    asunto,
				    msg
				);
		}

		return rutaGuardada;
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

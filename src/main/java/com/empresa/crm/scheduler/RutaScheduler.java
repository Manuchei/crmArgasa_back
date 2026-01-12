package com.empresa.crm.scheduler;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.empresa.crm.entities.Ruta;
import com.empresa.crm.repositories.RutaRepository;
import com.empresa.crm.services.EmailService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RutaScheduler {

	private final RutaRepository rutaRepository;
	private final EmailService emailService;

	// 📅 Enviar rutas del día todos los días a las 08:00
	@Scheduled(cron = "0 0 8 * * *", zone = "Europe/Madrid")
	public void enviarRutasDiarias() {
		LocalDate hoy = LocalDate.now();
		List<Ruta> rutasHoy = rutaRepository.findByFecha(hoy);

		if (rutasHoy.isEmpty())
			return;

		Map<String, List<Ruta>> rutasPorTransportista = rutasHoy.stream()
				.collect(Collectors.groupingBy(Ruta::getNombreTransportista));

		rutasPorTransportista.forEach((transportista, rutas) -> {
			String email = rutas.get(0).getEmailTransportista();

			if (email == null || email.isBlank())
				return;

			String asunto = "Rutas del día " + hoy + " - " + transportista;
			String html = construirHtmlRutas(hoy, transportista, rutas, false);

			emailService.enviarCorreoHtml(email, asunto, html);
		});
	}

	// 🔄 Actualizaciones cuando se modifica una ruta del día
	public void enviarActualizacionHoy(String transportista) {
		LocalDate hoy = LocalDate.now();

		List<Ruta> rutasHoyTransp = rutaRepository.findByFechaAndNombreTransportistaIgnoreCase(hoy, transportista);

		if (rutasHoyTransp.isEmpty())
			return;

		String email = rutasHoyTransp.get(0).getEmailTransportista();
		if (email == null || email.isBlank())
			return;

		String asunto = "ACTUALIZACIÓN – Rutas del día " + hoy + " - " + transportista;
		String html = construirHtmlRutas(hoy, transportista, rutasHoyTransp, true);

		emailService.enviarCorreoHtml(email, asunto, html);
	}

	// 🧠 Construcción del correo HTML
	private String construirHtmlRutas(LocalDate fecha, String transportista, List<Ruta> rutas,
			boolean esActualizacion) {

		String titulo = esActualizacion ? "Actualización de rutas" : "Rutas asignadas";

		StringBuilder sb = new StringBuilder();

		sb.append("<h2>").append(titulo).append(" - ").append(fecha).append("</h2>");
		sb.append("<p>Transportista: <strong>").append(transportista).append("</strong></p>");

		sb.append("<table border='1' cellpadding='6' cellspacing='0' style='border-collapse:collapse;width:100%;'>");
		sb.append("<thead style='background:#f0f0f0;'>").append("<tr>").append("<th>Origen</th>")
				.append("<th>Destino</th>").append("<th>Estado</th>").append("<th>Observaciones</th>").append("</tr>")
				.append("</thead><tbody>");

		for (Ruta r : rutas) {
			sb.append("<tr>")
			  .append("<td>").append(ns(r.getOrigen())).append("</td>")
			  .append("<td>").append(ns(r.getDestino())).append("</td>")
			  .append("<td>").append(ns(r.getTarea())).append("</td>")
			  .append("<td>").append(ns(r.getEstado())).append("</td>")
			  .append("<td>").append(ns(r.getObservaciones())).append("</td>")
			  .append("</tr>");

		}

		sb.append("</tbody></table>");

		sb.append("<p style='margin-top:10px;font-size:12px;color:#666;'>")
				.append("Correo generado automáticamente por el sistema.").append("</p>");

		return sb.toString();
	}

	private String ns(Object o) {
		return o == null ? "" : o.toString();
	}
	
	public void enviarRutasDeFecha(String transportista, LocalDate fecha) {
	    List<Ruta> rutas = rutaRepository.findByFechaAndNombreTransportistaIgnoreCase(fecha, transportista);

	    if (rutas.isEmpty()) return;

	    String email = rutas.get(0).getEmailTransportista();
	    if (email == null || email.isBlank()) return;

	    String asunto = "Rutas del día " + fecha + " - " + transportista;
	    String html = construirHtmlRutas(fecha, transportista, rutas, false);

	    emailService.enviarCorreoHtml(email, asunto, html);
	}

}

package com.empresa.crm.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.empresa.crm.dto.PushMessageDTO;
import com.empresa.crm.entities.Llamada;
import com.empresa.crm.entities.PushSubscription;
import com.empresa.crm.repositories.LlamadaRepository;
import com.empresa.crm.services.PushSubscriptionService;
import com.empresa.crm.services.WebPushService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LlamadaReminderScheduler {

	private final LlamadaRepository llamadaRepository;
	private final PushSubscriptionService pushSubscriptionService;
	private final WebPushService webPushService;

	@Scheduled(cron = "0 * * * * *", zone = "Europe/Madrid")
	public void avisarLlamadasEnCincoMinutos() {
		LocalDateTime ahora = LocalDateTime.now();
		LocalDateTime inicio = ahora.plusMinutes(5).withSecond(0).withNano(0);
		LocalDateTime fin = inicio.plusMinutes(1);

		List<Llamada> candidatas = llamadaRepository.findByFechaBetween(inicio, fin);

		for (Llamada llamada : candidatas) {
			if (llamada.isRecordatorioEnviado()) {
				continue;
			}

			if (!"pendiente".equalsIgnoreCase(llamada.getEstado())) {
				continue;
			}

			List<PushSubscription> subs = pushSubscriptionService.getActiveByEmpresa(llamada.getEmpresa());

			String motivo = llamada.getMotivo() == null ? "Llamada programada" : llamada.getMotivo();

			PushMessageDTO msg = new PushMessageDTO("Llamada en 5 minutos",
					motivo + " • " + llamada.getFecha().toLocalTime(), "/app/calendario");

			for (PushSubscription sub : subs) {
				try {
					webPushService.sendNotification(sub, msg);
				} catch (Exception e) {
					// aquí puedes desactivar la suscripción si falla repetidamente
				}
			}

			llamada.setRecordatorioEnviado(true);
			llamadaRepository.save(llamada);
		}
	}
}
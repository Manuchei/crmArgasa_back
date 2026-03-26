package com.empresa.crm.serviceImpl;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.Base64;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.empresa.crm.dto.PushMessageDTO;
import com.empresa.crm.entities.PushSubscription;
import com.empresa.crm.services.WebPushService;
import com.fasterxml.jackson.databind.ObjectMapper;

import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Utils;

@Service
public class WebPushServiceImpl implements WebPushService {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final PushService pushService;

	public WebPushServiceImpl(@Value("${app.push.public-key}") String publicKey,
			@Value("${app.push.private-key}") String privateKey, @Value("${app.push.subject}") String subject)
			throws GeneralSecurityException, JoseException {

		if (Security.getProvider("BC") == null) {
			Security.addProvider(new BouncyCastleProvider());
		}

		this.pushService = new PushService();
		this.pushService.setPublicKey(Utils.loadPublicKey(publicKey));
		this.pushService.setPrivateKey(Utils.loadPrivateKey(privateKey));
		this.pushService.setSubject(subject);
	}

	@Override
	public void sendNotification(PushSubscription subscription, PushMessageDTO message) {
		try {
			String payload = objectMapper.writeValueAsString(message);

			byte[] authSecret = Base64.getUrlDecoder().decode(subscription.getAuth());

			Notification notification = new Notification(subscription.getEndpoint(),
					Utils.loadPublicKey(subscription.getP256dh()), authSecret,
					payload.getBytes(StandardCharsets.UTF_8));

			pushService.send(notification);

		} catch (Exception e) {
			throw new RuntimeException("Error enviando push notification", e);
		}
	}
}
package com.empresa.crm.serviceImpl;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;

import com.empresa.crm.services.EmailService;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

	private final JavaMailSender mailSender;

	@Override
	public void enviarCorreo(String para, String asunto, String textoPlano) {
		try {
			MimeMessage msg = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(msg, false, "UTF-8");

			helper.setTo(para);
			helper.setSubject(asunto);
			helper.setText(textoPlano);

			mailSender.send(msg);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void enviarCorreoHtml(String para, String asunto, String html) {
		try {
			MimeMessage msg = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");

			helper.setTo(para);
			helper.setSubject(asunto);
			helper.setText(html, true);

			mailSender.send(msg);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

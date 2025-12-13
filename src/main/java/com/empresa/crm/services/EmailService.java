package com.empresa.crm.services;

public interface EmailService {
	void enviarCorreo(String para, String asunto, String mensaje);

	void enviarCorreoHtml(String para, String asunto, String contenidoHtml);

}

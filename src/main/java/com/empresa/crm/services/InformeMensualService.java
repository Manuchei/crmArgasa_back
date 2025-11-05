package com.empresa.crm.services;

import com.empresa.crm.dto.ResumenMensualDTO;

public interface InformeMensualService {
	ResumenMensualDTO generarResumen(String empresa, int year, int month);
}

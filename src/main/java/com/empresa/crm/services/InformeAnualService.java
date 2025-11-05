package com.empresa.crm.services;

import com.empresa.crm.dto.ResumenAnualDTO;

public interface InformeAnualService {
	ResumenAnualDTO generarResumenAnual(String empresa, int year);
}
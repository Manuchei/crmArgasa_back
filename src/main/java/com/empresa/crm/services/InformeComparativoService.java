package com.empresa.crm.services;

import com.empresa.crm.dto.ResumenComparativoDTO;

public interface InformeComparativoService {
	ResumenComparativoDTO generarComparativoAnual(int year);
}

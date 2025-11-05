package com.empresa.crm.serviceImpl;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

import com.empresa.crm.dto.ResumenAnualDTO;
import com.empresa.crm.dto.ResumenComparativoDTO;
import com.empresa.crm.services.InformeAnualService;
import com.empresa.crm.services.InformeComparativoService;

@Service
public class InformeComparativoServiceImpl implements InformeComparativoService {

	private final InformeAnualService informeAnualService;

	public InformeComparativoServiceImpl(InformeAnualService informeAnualService) {
		this.informeAnualService = informeAnualService;
	}

	@Override
	public ResumenComparativoDTO generarComparativoAnual(int year) {
		ResumenAnualDTO informeArgasa = informeAnualService.generarResumenAnual("argasa", year);
		ResumenAnualDTO informeLuga = informeAnualService.generarResumenAnual("luga", year);

		List<ResumenAnualDTO> informes = Arrays.asList(informeArgasa, informeLuga);
		return new ResumenComparativoDTO(year, informes);
	}
}
package com.empresa.crm.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumenComparativoDTO {
	private int año;
	private List<ResumenAnualDTO> informes;
}

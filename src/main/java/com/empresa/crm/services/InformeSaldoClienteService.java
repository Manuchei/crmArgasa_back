package com.empresa.crm.services;

import java.time.LocalDate;
import java.util.List;

import com.empresa.crm.dto.HistorialSaldoResponseDTO;
import com.empresa.crm.dto.HistorialTContableResponseDTO;
import com.empresa.crm.dto.SaldoPendienteClienteDTO;

public interface InformeSaldoClienteService {

	HistorialSaldoResponseDTO obtenerHistorialSaldo(Long clienteId);

	HistorialTContableResponseDTO obtenerHistorialTContable(Long clienteId);

	List<HistorialSaldoResponseDTO> obtenerHistorialSaldoFiltrado(Long clienteId, LocalDate fechaInicio,
			LocalDate fechaFin, String empresa);

	List<SaldoPendienteClienteDTO> obtenerSaldosPendientes(String empresa);

}
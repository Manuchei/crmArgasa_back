package com.empresa.crm.serviceImpl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.empresa.crm.dto.HistorialSaldoMovimientoDTO;
import com.empresa.crm.dto.HistorialSaldoResponseDTO;
import com.empresa.crm.dto.HistorialTContableResponseDTO;
import com.empresa.crm.dto.TContableLineaDTO;
import com.empresa.crm.entities.Cliente;
import com.empresa.crm.entities.PagoCliente;
import com.empresa.crm.entities.Trabajo;
import com.empresa.crm.repositories.ClienteRepository;
import com.empresa.crm.repositories.PagoClienteRepository;
import com.empresa.crm.repositories.TrabajoRepository;
import com.empresa.crm.services.InformeSaldoClienteService;

@Service
public class InformeSaldoClienteServiceImpl implements InformeSaldoClienteService {

	private final ClienteRepository clienteRepository;
	private final TrabajoRepository trabajoRepository;
	private final PagoClienteRepository pagoClienteRepository;

	public InformeSaldoClienteServiceImpl(ClienteRepository clienteRepository, TrabajoRepository trabajoRepository,
			PagoClienteRepository pagoClienteRepository) {
		this.clienteRepository = clienteRepository;
		this.trabajoRepository = trabajoRepository;
		this.pagoClienteRepository = pagoClienteRepository;
	}

	@Override
	public HistorialSaldoResponseDTO obtenerHistorialSaldo(Long clienteId) {
		Cliente cliente = clienteRepository.findById(clienteId)
				.orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

		List<Trabajo> trabajos = trabajoRepository.findByClienteIdOrderByFechaAscIdAsc(clienteId);
		List<PagoCliente> pagos = pagoClienteRepository.findByClienteIdOrderByFechaAscIdAsc(clienteId);

		return construirHistorialSaldo(cliente, trabajos, pagos);
	}

	@Override
	public HistorialTContableResponseDTO obtenerHistorialTContable(Long clienteId) {
		Cliente cliente = clienteRepository.findById(clienteId)
				.orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

		List<TContableLineaDTO> debe = new ArrayList<>();
		List<TContableLineaDTO> haber = new ArrayList<>();

		List<Trabajo> trabajos = trabajoRepository.findByClienteIdOrderByFechaAscIdAsc(clienteId);
		for (Trabajo t : trabajos) {
			debe.add(new TContableLineaDTO(t.getFecha(),
					t.getDescripcion() != null ? t.getDescripcion() : "Trabajo #" + t.getId(), safe(t.getImporte())));
		}

		List<PagoCliente> pagos = pagoClienteRepository.findByClienteIdOrderByFechaAscIdAsc(clienteId);
		for (PagoCliente p : pagos) {
			String concepto = "Pago";
			if (p.getMetodo() != null && !p.getMetodo().isBlank()) {
				concepto += " - " + p.getMetodo();
			}

			haber.add(new TContableLineaDTO(p.getFecha(), concepto, safe(p.getImporte())));
		}

		debe.sort(Comparator.comparing(TContableLineaDTO::getFecha, Comparator.nullsLast(Comparator.naturalOrder())));

		haber.sort(Comparator.comparing(TContableLineaDTO::getFecha, Comparator.nullsLast(Comparator.naturalOrder())));

		double totalDebe = debe.stream().mapToDouble(linea -> safe(linea.getImporte())).sum();
		double totalHaber = haber.stream().mapToDouble(linea -> safe(linea.getImporte())).sum();

		double saldoFinal = totalDebe - totalHaber;

		String estadoSaldo = "SALDADO";
		if (saldoFinal > 0)
			estadoSaldo = "PENDIENTE";
		else if (saldoFinal < 0)
			estadoSaldo = "A_FAVOR";

		return new HistorialTContableResponseDTO(cliente.getId(), cliente.getNombreApellidos(), cliente.getEmpresa(),
				debe, haber, totalDebe, totalHaber, saldoFinal, estadoSaldo);
	}

	@Override
	public List<HistorialSaldoResponseDTO> obtenerHistorialSaldoFiltrado(Long clienteId, LocalDate fechaInicio,
			LocalDate fechaFin, String empresa) {

		List<HistorialSaldoResponseDTO> resultado = new ArrayList<>();

		// 🔴 empresa obligatoria
		if (empresa == null || empresa.isBlank()) {
			throw new RuntimeException("La empresa es obligatoria");
		}

		// 🔵 CASO 1: Cliente concreto
		if (clienteId != null) {
			Cliente cliente = clienteRepository.findById(clienteId)
					.orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

			// 🚫 si no pertenece a la empresa → no devolver nada
			if (cliente.getEmpresa() == null || !cliente.getEmpresa().equalsIgnoreCase(empresa)) {
				return resultado;
			}

			List<Trabajo> trabajos = obtenerTrabajosFiltrados(clienteId, fechaInicio, fechaFin);
			List<PagoCliente> pagos = obtenerPagosFiltrados(clienteId, fechaInicio, fechaFin);

			if (!trabajos.isEmpty() || !pagos.isEmpty()) {
				resultado.add(construirHistorialSaldo(cliente, trabajos, pagos));
			}

			return resultado;
		}

		// 🔵 CASO 2: Todos los clientes → FILTRADOS POR EMPRESA
		List<Cliente> clientes = clienteRepository.findAll();

		for (Cliente cliente : clientes) {

			// 🔴 filtro clave por empresa
			if (cliente.getEmpresa() == null || !cliente.getEmpresa().equalsIgnoreCase(empresa)) {
				continue;
			}

			Long idCliente = cliente.getId();

			List<Trabajo> trabajos = obtenerTrabajosFiltrados(idCliente, fechaInicio, fechaFin);
			List<PagoCliente> pagos = obtenerPagosFiltrados(idCliente, fechaInicio, fechaFin);

			if (trabajos.isEmpty() && pagos.isEmpty()) {
				continue;
			}

			resultado.add(construirHistorialSaldo(cliente, trabajos, pagos));
		}

		return resultado;
	}

	private HistorialSaldoResponseDTO construirHistorialSaldo(Cliente cliente, List<Trabajo> trabajos,
			List<PagoCliente> pagos) {

		List<HistorialSaldoMovimientoDTO> movimientos = new ArrayList<>();

		for (Trabajo t : trabajos) {
			movimientos.add(new HistorialSaldoMovimientoDTO(t.getFecha(), "CARGO",
					t.getDescripcion() != null ? t.getDescripcion() : "Trabajo #" + t.getId(), safe(t.getImporte()),
					0.0, 0.0));
		}

		for (PagoCliente p : pagos) {
			String concepto = "Pago";
			if (p.getMetodo() != null && !p.getMetodo().isBlank()) {
				concepto += " - " + p.getMetodo();
			}

			movimientos.add(
					new HistorialSaldoMovimientoDTO(p.getFecha(), "ABONO", concepto, 0.0, safe(p.getImporte()), 0.0));
		}

		movimientos.sort(Comparator
				.comparing(HistorialSaldoMovimientoDTO::getFecha, Comparator.nullsLast(Comparator.naturalOrder()))
				.thenComparing(HistorialSaldoMovimientoDTO::getTipo));

		double saldo = 0.0;
		for (HistorialSaldoMovimientoDTO mov : movimientos) {
			saldo += safe(mov.getCargo());
			saldo -= safe(mov.getAbono());
			mov.setSaldoAcumulado(saldo);
		}

		String estadoSaldo = "SALDADO";
		if (saldo > 0)
			estadoSaldo = "PENDIENTE";
		else if (saldo < 0)
			estadoSaldo = "A_FAVOR";

		return new HistorialSaldoResponseDTO(cliente.getId(), cliente.getNombreApellidos(), cliente.getEmpresa(), saldo,
				estadoSaldo, movimientos);
	}

	private List<Trabajo> obtenerTrabajosFiltrados(Long clienteId, LocalDate fechaInicio, LocalDate fechaFin) {
		if (fechaInicio != null && fechaFin != null) {
			return trabajoRepository.findByClienteIdAndFechaBetweenOrderByFechaAscIdAsc(clienteId, fechaInicio,
					fechaFin);
		}
		if (fechaInicio != null) {
			return trabajoRepository.findByClienteIdAndFechaGreaterThanEqualOrderByFechaAscIdAsc(clienteId,
					fechaInicio);
		}
		if (fechaFin != null) {
			return trabajoRepository.findByClienteIdAndFechaLessThanEqualOrderByFechaAscIdAsc(clienteId, fechaFin);
		}
		return trabajoRepository.findByClienteIdOrderByFechaAscIdAsc(clienteId);
	}

	private List<PagoCliente> obtenerPagosFiltrados(Long clienteId, LocalDate fechaInicio, LocalDate fechaFin) {
		if (fechaInicio != null && fechaFin != null) {
			return pagoClienteRepository.findByClienteIdAndFechaBetweenOrderByFechaAscIdAsc(clienteId, fechaInicio,
					fechaFin);
		}
		if (fechaInicio != null) {
			return pagoClienteRepository.findByClienteIdAndFechaGreaterThanEqualOrderByFechaAscIdAsc(clienteId,
					fechaInicio);
		}
		if (fechaFin != null) {
			return pagoClienteRepository.findByClienteIdAndFechaLessThanEqualOrderByFechaAscIdAsc(clienteId, fechaFin);
		}
		return pagoClienteRepository.findByClienteIdOrderByFechaAscIdAsc(clienteId);
	}

	private double safe(Double value) {
		return value != null ? value : 0.0;
	}
}
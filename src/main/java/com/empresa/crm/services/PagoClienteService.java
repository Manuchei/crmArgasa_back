package com.empresa.crm.services;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.empresa.crm.dto.PagoClienteComprobanteDTO;
import com.empresa.crm.dto.PagoClienteCreateRequest;
import com.empresa.crm.entities.Cliente;
import com.empresa.crm.entities.PagoCliente;
import com.empresa.crm.repositories.ClienteRepository;
import com.empresa.crm.repositories.PagoClienteRepository;

@Service
public class PagoClienteService {

	private final PagoClienteRepository pagoRepo;
	private final ClienteRepository clienteRepo;

	public PagoClienteService(PagoClienteRepository pagoRepo, ClienteRepository clienteRepo) {
		this.pagoRepo = pagoRepo;
		this.clienteRepo = clienteRepo;
	}

	public List<PagoCliente> listarPorCliente(Long clienteId) {
		return pagoRepo.findByClienteIdOrderByFechaAscIdAsc(clienteId);
	}

	@Transactional
	public PagoCliente crearPago(Long clienteId, PagoClienteCreateRequest req) {
		Cliente c = clienteRepo.findById(clienteId).orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

		double importe = safe(req.getImporte());
		if (importe <= 0) {
			throw new RuntimeException("Importe inválido");
		}

		String metodo = trim(req.getMetodo());
		if (metodo.isEmpty()) {
			throw new RuntimeException("Método inválido");
		}

		PagoCliente p = new PagoCliente();
		p.setCliente(c);

		// importante: guardar la empresa del cliente
		p.setEmpresa(c.getEmpresa());

		p.setFecha(req.getFecha() != null ? req.getFecha() : LocalDate.now());
		p.setImporte(importe);
		p.setMetodo(metodo);
		p.setObservaciones(nullIfBlank(req.getObservaciones()));

		return pagoRepo.save(p);
	}

	public PagoClienteComprobanteDTO obtenerComprobante(Long pagoId) {
		PagoCliente p = pagoRepo.findById(pagoId).orElseThrow(() -> new RuntimeException("Pago no encontrado"));

		Cliente c = p.getCliente();

		PagoClienteComprobanteDTO dto = new PagoClienteComprobanteDTO();
		dto.setId(p.getId());
		dto.setEmpresa(p.getEmpresa());
		dto.setFecha(p.getFecha());
		dto.setImporte(safe(p.getImporte()));
		dto.setMetodo(nullIfBlank(p.getMetodo()));
		dto.setObservaciones(nullIfBlank(p.getObservaciones()));

		if (c != null) {
			dto.setClienteId(c.getId());
			dto.setClienteNombreApellidos(c.getNombreApellidos());
			dto.setClienteCifDni(c.getCifDni());
			dto.setClienteDireccion(c.getDireccion());
			dto.setClienteCodigoPostal(c.getCodigoPostal());
			dto.setClientePoblacion(c.getPoblacion());
			dto.setClienteProvincia(c.getProvincia());
			dto.setClienteTelefono(c.getTelefono());
			dto.setClienteMovil(c.getMovil());
			dto.setClienteEmail(c.getEmail());
		}

		EmpresaInfo empresaInfo = getEmpresaInfo(p.getEmpresa());
		dto.setEmpresaNombre(empresaInfo.nombre);
		dto.setEmpresaCif(empresaInfo.cif);
		dto.setEmpresaDireccion(empresaInfo.direccion);
		dto.setEmpresaCodigoPostal(empresaInfo.codigoPostal);
		dto.setEmpresaPoblacion(empresaInfo.poblacion);
		dto.setEmpresaProvincia(empresaInfo.provincia);
		dto.setEmpresaTelefono(empresaInfo.telefono);
		dto.setEmpresaEmail(empresaInfo.email);
		dto.setEmpresaLogoUrl(empresaInfo.logoUrl);

		return dto;
	}

	@Transactional
	public void eliminarPago(Long pagoId) {
		PagoCliente p = pagoRepo.findById(pagoId).orElse(null);
		if (p == null) {
			throw new RuntimeException("Pago no encontrado");
		}

		pagoRepo.deleteById(pagoId);
	}

	private double safe(Double v) {
		return v != null ? v : 0.0;
	}

	private String trim(String s) {
		return s == null ? "" : s.trim();
	}

	private String nullIfBlank(String s) {
		s = trim(s);
		return s.isEmpty() ? null : s;
	}

	private EmpresaInfo getEmpresaInfo(String empresa) {
		String emp = trim(empresa).toUpperCase();

		if ("ARGASA".equals(emp)) {
			return new EmpresaInfo("Argasa Garrido S.L.", "B36879617", "Rúa Pintor Laxeiro Nº15 Bajo", "36211", "Vigo",
					"Pontevedra", "607472159", "argasaluis@gmail.com", "/assets/logos/argasa.png");
		}

		if ("ELECTROLUGA".equals(emp) || "LUGA".equals(emp)) {
			return new EmpresaInfo("ELECTROLUGA, S.L.U", "B42722389", "Rúa Pintor Laxeiro Nº15 Bajo", "36211", "Vigo",
					"Pontevedra", "607472159", "electrolugaslu@gmail.com", "/assets/logos/luga.png");
		}

		return new EmpresaInfo(emp, "", "", "", "", "", "", "", "");
	}

	private static class EmpresaInfo {
		private final String nombre;
		private final String cif;
		private final String direccion;
		private final String codigoPostal;
		private final String poblacion;
		private final String provincia;
		private final String telefono;
		private final String email;
		private final String logoUrl;

		public EmpresaInfo(String nombre, String cif, String direccion, String codigoPostal, String poblacion,
				String provincia, String telefono, String email, String logoUrl) {
			this.nombre = nombre;
			this.cif = cif;
			this.direccion = direccion;
			this.codigoPostal = codigoPostal;
			this.poblacion = poblacion;
			this.provincia = provincia;
			this.telefono = telefono;
			this.email = email;
			this.logoUrl = logoUrl;
		}
	}
}
package com.empresa.crm.serviceImpl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.empresa.crm.entities.AlbaranProveedor;
import com.empresa.crm.entities.FacturaProveedor;
import com.empresa.crm.entities.LineaAlbaranProveedor;
import com.empresa.crm.entities.LineaFacturaProveedor;
import com.empresa.crm.repositories.AlbaranProveedorRepository;
import com.empresa.crm.repositories.FacturaProveedorRepository;
import com.empresa.crm.services.FacturaProveedorService;
import com.empresa.crm.tenant.TenantContext;

@Service
public class FacturaProveedorServiceImpl implements FacturaProveedorService {

	private final FacturaProveedorRepository facturaRepo;
	private final AlbaranProveedorRepository albaranRepo;

	public FacturaProveedorServiceImpl(FacturaProveedorRepository facturaRepo, AlbaranProveedorRepository albaranRepo) {
		this.facturaRepo = facturaRepo;
		this.albaranRepo = albaranRepo;
	}

	@Override
	public List<FacturaProveedor> findAll() {
		String empresa = TenantContext.get();
		List<FacturaProveedor> facturas = facturaRepo.findByEmpresa(empresa);
		inicializarLineas(facturas);
		return facturas;
	}

	@Override
	public FacturaProveedor findById(Long id) {
		String empresa = TenantContext.get();
		FacturaProveedor factura = facturaRepo.findByIdAndEmpresa(id, empresa).orElse(null);

		if (factura != null && factura.getLineas() != null) {
			factura.getLineas().size();
		}

		return factura;
	}

	@Override
	public FacturaProveedor generarFactura(Long proveedorId, String empresa) {
		throw new RuntimeException("La factura de proveedor debe generarse desde un albarán confirmado");
	}

	@Override
	@Transactional
	public FacturaProveedor generarFacturaDesdeAlbaran(Long albaranId) {
		String empresa = TenantContext.get();

		if (empresa == null || empresa.isBlank()) {
			throw new RuntimeException("Empresa no seleccionada");
		}

		AlbaranProveedor albaran = albaranRepo.findById(albaranId)
				.orElseThrow(() -> new RuntimeException("Albarán no encontrado"));

		if (!empresa.equalsIgnoreCase(albaran.getEmpresa())) {
			throw new RuntimeException("El albarán no pertenece a la empresa seleccionada");
		}

		if (albaran.getProveedor() == null) {
			throw new RuntimeException("El albarán no tiene proveedor asociado");
		}

		if (!albaran.isConfirmado()) {
			throw new RuntimeException("Solo se puede generar factura de un albarán confirmado");
		}

		if (albaran.getLineas() == null || albaran.getLineas().isEmpty()) {
			throw new RuntimeException("El albarán no tiene líneas");
		}

		Optional<FacturaProveedor> facturaExistente = facturaRepo.findByAlbaranProveedorId(albaranId);
		if (facturaExistente.isPresent()) {
			FacturaProveedor existente = facturaExistente.get();
			if (existente.getLineas() != null)
				existente.getLineas().size();
			return existente;
		}

		FacturaProveedor factura = new FacturaProveedor();
		factura.setProveedor(albaran.getProveedor());
		factura.setAlbaranProveedor(albaran);
		factura.setEmpresa(albaran.getEmpresa());
		factura.setFechaEmision(LocalDate.now());
		factura.setEstado("BORRADOR");
		factura.setPagada(false);
		factura.setNumeroInterno(generarNumeroInterno(albaran.getEmpresa()));
		factura.setNumeroFacturaProveedor(albaran.getNumeroProveedor());

		List<LineaFacturaProveedor> lineasFactura = new ArrayList<>();

		for (LineaAlbaranProveedor lineaAlbaran : albaran.getLineas()) {
			if (lineaAlbaran == null)
				continue;

			LineaFacturaProveedor lineaFactura = new LineaFacturaProveedor();
			lineaFactura.setFactura(factura);
			lineaFactura.setTipoOrigen(lineaAlbaran.getTipo() != null ? lineaAlbaran.getTipo() : "ALBARAN_LINEA");
			lineaFactura.setOrigenId(lineaAlbaran.getId());
			lineaFactura.setDescripcion(lineaAlbaran.getDescripcion());
			lineaFactura.setCantidad(lineaAlbaran.getUnidades() != null ? lineaAlbaran.getUnidades() : 0.0);
			lineaFactura.setPrecioUnitario(lineaAlbaran.getPrecio() != null ? lineaAlbaran.getPrecio() : 0.0);
			lineaFactura.setDescuentoPct(lineaAlbaran.getDtoPct() != null ? lineaAlbaran.getDtoPct() : 0.0);
			lineaFactura.setIvaPct(0.0);
			lineaFactura.recalcular();

			lineasFactura.add(lineaFactura);
		}

		factura.setLineas(lineasFactura);
		factura.recalcularTotales();

		return facturaRepo.save(factura);
	}

	@Override
	@Transactional
	public FacturaProveedor guardarBorrador(Long facturaId, FacturaProveedor facturaEditada) {
		String empresa = TenantContext.get();

		FacturaProveedor factura = facturaRepo.findByIdAndEmpresa(facturaId, empresa)
				.orElseThrow(() -> new RuntimeException("Factura no encontrada"));

		if (!"BORRADOR".equalsIgnoreCase(factura.getEstado())) {
			throw new RuntimeException("Solo se puede modificar una factura en estado BORRADOR");
		}

		if (facturaEditada.getFechaEmision() != null) {
			factura.setFechaEmision(facturaEditada.getFechaEmision());
		}

		factura.setNumeroFacturaProveedor(facturaEditada.getNumeroFacturaProveedor());

		factura.getLineas().clear();

		if (facturaEditada.getLineas() != null) {
			for (LineaFacturaProveedor lineaEditada : facturaEditada.getLineas()) {
				if (lineaEditada == null)
					continue;

				LineaFacturaProveedor linea = new LineaFacturaProveedor();
				linea.setFactura(factura);
				linea.setTipoOrigen(lineaEditada.getTipoOrigen());
				linea.setOrigenId(lineaEditada.getOrigenId());
				linea.setDescripcion(lineaEditada.getDescripcion());
				linea.setCantidad(lineaEditada.getCantidad() != null ? lineaEditada.getCantidad() : 0.0);
				linea.setPrecioUnitario(
						lineaEditada.getPrecioUnitario() != null ? lineaEditada.getPrecioUnitario() : 0.0);
				linea.setDescuentoPct(lineaEditada.getDescuentoPct() != null ? lineaEditada.getDescuentoPct() : 0.0);
				linea.setIvaPct(lineaEditada.getIvaPct() != null ? lineaEditada.getIvaPct() : 0.0);
				linea.recalcular();

				factura.getLineas().add(linea);
			}
		}

		factura.recalcularTotales();
		return facturaRepo.save(factura);
	}

	@Override
	@Transactional
	public FacturaProveedor emitirFactura(Long facturaId) {
		String empresa = TenantContext.get();

		FacturaProveedor factura = facturaRepo.findByIdAndEmpresa(facturaId, empresa)
				.orElseThrow(() -> new RuntimeException("Factura no encontrada"));

		if (!"BORRADOR".equalsIgnoreCase(factura.getEstado())) {
			throw new RuntimeException("Solo se puede emitir una factura en BORRADOR");
		}

		factura.setEstado("EMITIDA");
		factura.setPagada(false);
		factura.recalcularTotales();

		return facturaRepo.save(factura);
	}

	@Override
	@Transactional
	public FacturaProveedor marcarComoPagada(Long facturaId) {
		String empresa = TenantContext.get();

		FacturaProveedor factura = facturaRepo.findByIdAndEmpresa(facturaId, empresa)
				.orElseThrow(() -> new RuntimeException("Factura no encontrada"));

		factura.setEstado("PAGADA");
		factura.setPagada(true);
		factura.recalcularTotales();

		return facturaRepo.save(factura);
	}

	@Override
	@Transactional
	public FacturaProveedor actualizarNumeroFacturaProveedor(Long facturaId, String numeroFacturaProveedor) {
		String empresa = TenantContext.get();

		FacturaProveedor factura = facturaRepo.findByIdAndEmpresa(facturaId, empresa)
				.orElseThrow(() -> new RuntimeException("Factura no encontrada"));

		if (!"BORRADOR".equalsIgnoreCase(factura.getEstado())) {
			throw new RuntimeException("Solo se puede modificar el número en estado BORRADOR");
		}

		factura.setNumeroFacturaProveedor(numeroFacturaProveedor);
		return facturaRepo.save(factura);
	}

	@Override
	public List<FacturaProveedor> findByEmpresa(String empresa) {
		List<FacturaProveedor> facturas = facturaRepo.findByEmpresa(TenantContext.get());
		inicializarLineas(facturas);
		return facturas;
	}

	@Override
	public List<FacturaProveedor> findByProveedor(Long proveedorId) {
		String empresa = TenantContext.get();
		List<FacturaProveedor> facturas = facturaRepo.findByProveedorIdAndEmpresa(proveedorId, empresa);
		inicializarLineas(facturas);
		return facturas;
	}

	private void inicializarLineas(List<FacturaProveedor> facturas) {
		for (FacturaProveedor factura : facturas) {
			if (factura != null && factura.getLineas() != null) {
				factura.getLineas().size();
			}
		}
	}

	private String generarNumeroInterno(String empresa) {
		int anio = LocalDate.now().getYear();
		String prefijoEmpresa = normalizarEmpresa(empresa);

		Optional<FacturaProveedor> ultimaFacturaOpt = facturaRepo.findTopByEmpresaOrderByIdDesc(empresa);

		int siguienteNumero = 1;

		if (ultimaFacturaOpt.isPresent()) {
			String ultimoNumeroInterno = ultimaFacturaOpt.get().getNumeroInterno();

			if (ultimoNumeroInterno != null && !ultimoNumeroInterno.isBlank()) {
				String[] partes = ultimoNumeroInterno.split("-");
				if (partes.length == 4) {
					try {
						int ultimoCorrelativo = Integer.parseInt(partes[3]);
						int ultimoAnio = Integer.parseInt(partes[2]);

						if (ultimoAnio == anio) {
							siguienteNumero = ultimoCorrelativo + 1;
						}
					} catch (NumberFormatException e) {
						siguienteNumero = 1;
					}
				}
			}
		}

		return String.format("FP-%s-%d-%04d", prefijoEmpresa, anio, siguienteNumero);
	}

	private String normalizarEmpresa(String empresa) {
		if (empresa == null || empresa.isBlank()) {
			return "GEN";
		}

		String empresaNormalizada = empresa.trim().toUpperCase();

		if (empresaNormalizada.equals("ARGASA")) {
			return "ARG";
		}

		if (empresaNormalizada.equals("LUGA") || empresaNormalizada.equals("ELECTROLUGA")) {
			return "LUG";
		}

		return empresaNormalizada.length() >= 3 ? empresaNormalizada.substring(0, 3) : empresaNormalizada;
	}

	@Override
	@Transactional
	public void eliminarBorrador(Long facturaId) {
		String empresa = TenantContext.get();

		FacturaProveedor factura = facturaRepo.findByIdAndEmpresa(facturaId, empresa)
				.orElseThrow(() -> new RuntimeException("Factura no encontrada"));

		if (!"BORRADOR".equalsIgnoreCase(factura.getEstado())) {
			throw new RuntimeException("Solo se puede eliminar una factura en borrador");
		}

		facturaRepo.delete(factura);
	}
}
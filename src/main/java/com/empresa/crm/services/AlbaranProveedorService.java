package com.empresa.crm.services;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.empresa.crm.entities.AlbaranProveedor;
import com.empresa.crm.entities.LineaAlbaranProveedor;
import com.empresa.crm.entities.Producto;
import com.empresa.crm.entities.Proveedor;
import com.empresa.crm.entities.Trabajo;
import com.empresa.crm.repositories.AlbaranProveedorRepository;
import com.empresa.crm.repositories.ProveedorRepository;
import com.empresa.crm.repositories.TrabajoRepository;
import com.empresa.crm.tenant.TenantContext;

@Service
public class AlbaranProveedorService {

	private final AlbaranProveedorRepository albaranRepo;
	private final ProveedorRepository proveedorRepo;
	private final TrabajoRepository trabajoRepo;

	public AlbaranProveedorService(AlbaranProveedorRepository albaranRepo, ProveedorRepository proveedorRepo,
			TrabajoRepository trabajoRepo) {
		this.albaranRepo = albaranRepo;
		this.proveedorRepo = proveedorRepo;
		this.trabajoRepo = trabajoRepo;
	}

	// =========================
	// MÉTODOS CORREGIDOS
	// =========================

	public List<AlbaranProveedor> findAll() {
		String empresa = TenantContext.get();
		return albaranRepo.findByEmpresaOrderByFechaEmisionDescIdDesc(empresa);
	}

	public List<AlbaranProveedor> findByProveedor(Long proveedorId) {
		String empresa = TenantContext.get();
		return albaranRepo.findByProveedorIdAndEmpresaOrderByFechaEmisionDescIdDesc(proveedorId, empresa);
	}

	public List<AlbaranProveedor> findByEmpresa() {
		String empresa = TenantContext.get();
		return albaranRepo.findByEmpresaOrderByFechaEmisionDescIdDesc(empresa);
	}

	public AlbaranProveedor findById(Long id) {
		String empresa = TenantContext.get();
		return albaranRepo.findByIdAndEmpresa(id, empresa).orElse(null);
	}

	@Transactional
	public AlbaranProveedor crearDesdeProveedor(Long proveedorId, String numeroProveedor, LocalDate fechaEmision) {
		String empresa = TenantContext.get();

		Proveedor p = proveedorRepo.findByIdAndEmpresa(proveedorId, empresa)
				.orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));

		AlbaranProveedor a = new AlbaranProveedor();
		a.setProveedor(p);
		a.setEmpresa(empresa);
		a.setFechaEmision(fechaEmision != null ? fechaEmision : LocalDate.now());

		// Número proveedor
		a.setNumeroProveedor(numeroProveedor != null ? numeroProveedor.trim() : null);

		// Número interno + generado
		String numeroInterno = generarNumeroInterno(empresa);
		a.setNumeroInterno(numeroInterno);

		if (a.getNumeroProveedor() != null && !a.getNumeroProveedor().isBlank()) {
			a.setNumeroGenerado(numeroInterno + " / " + a.getNumeroProveedor());
		} else {
			a.setNumeroGenerado(numeroInterno);
		}

		// SNAPSHOT PROVEEDOR
		a.setNombre(p.getNombre());
		a.setApellido(p.getApellido());
		a.setOficio(p.getOficio());
		a.setCif(p.getCif());
		a.setDireccion(p.getDireccion());
		a.setCodigoPostal(p.getCodigoPostal());
		a.setLocalidad(p.getLocalidad());
		a.setProvincia(p.getProvincia());
		a.setPais(p.getPais());
		a.setTelefono(p.getTelefono());
		a.setEmail(p.getEmail());
		a.setContacto(p.getContacto());
		a.setDatosBancarios(p.getDatosBancarios());
		a.setNotas(p.getNotas());
		a.setContactos(p.getContactos());

		// ===== TRABAJOS -> LÍNEAS =====
		List<Trabajo> trabajos = trabajoRepo.findByProveedorIdAndEmpresa(proveedorId, empresa);

		if (trabajos != null) {
			for (Trabajo t : trabajos) {
				if (t == null)
					continue;

				String desc = t.getDescripcion() != null ? t.getDescripcion().trim() : "";
				if (desc.isBlank())
					continue;

				LineaAlbaranProveedor l = new LineaAlbaranProveedor();
				l.setAlbaran(a);
				l.setEmpresa(empresa);
				l.setTipo("TRABAJO");
				l.setCodigo(null);
				l.setDescripcion(desc);
				l.setUnidades((double) safeInt(t.getUnidades(), 1));
				l.setPrecio(safe(t.getPrecioUnitario()));
				l.setDtoPct(safe(t.getDescuento()));
				l.recalcular();

				a.getLineas().add(l);
			}
		}

		// ===== PRODUCTOS -> LÍNEAS =====
		if (p.getProductos() != null) {
			for (Producto prod : p.getProductos()) {
				if (prod == null)
					continue;

				String nombre = prod.getNombre() != null ? prod.getNombre().trim() : "";
				if (nombre.isBlank())
					continue;

				LineaAlbaranProveedor l = new LineaAlbaranProveedor();
				l.setAlbaran(a);
				l.setEmpresa(empresa);
				l.setTipo("PRODUCTO");
				l.setCodigo(prod.getCodigo());
				l.setDescripcion(nombre);
				l.setUnidades((double) Math.max(prod.getStock(), 0));
				l.setPrecio(safe(prod.getPrecioSinIva()));
				l.setDtoPct(0.0);
				l.recalcular();

				a.getLineas().add(l);
			}
		}

		a.recalcularTotales();
		return albaranRepo.save(a);
	}

	@Transactional
	public AlbaranProveedor save(AlbaranProveedor albaran) {
		if (albaran.getLineas() != null) {
			for (LineaAlbaranProveedor l : albaran.getLineas()) {
				if (l == null)
					continue;

				l.setAlbaran(albaran);

				if (l.getEmpresa() == null || l.getEmpresa().isBlank()) {
					l.setEmpresa(albaran.getEmpresa());
				}

				if (l.getDtoPct() == null) {
					l.setDtoPct(0.0);
				}

				l.recalcular();
			}
		}

		albaran.recalcularTotales();
		return albaranRepo.save(albaran);
	}

	@Transactional
	public void deleteById(Long id) {
		albaranRepo.deleteById(id);
	}

	@Transactional
	public AlbaranProveedor agregarLinea(Long albaranId, LineaAlbaranProveedor linea) {
		AlbaranProveedor a = findById(albaranId);
		if (a == null)
			throw new RuntimeException("Albarán no encontrado");
		if (linea == null)
			throw new RuntimeException("Línea inválida");

		linea.setAlbaran(a);

		if (linea.getEmpresa() == null || linea.getEmpresa().isBlank()) {
			linea.setEmpresa(a.getEmpresa());
		}

		if (linea.getDtoPct() == null) {
			linea.setDtoPct(0.0);
		}

		linea.recalcular();

		a.getLineas().add(linea);
		a.recalcularTotales();

		return albaranRepo.save(a);
	}

	@Transactional
	public AlbaranProveedor eliminarLinea(Long albaranId, Long lineaId) {
		AlbaranProveedor a = findById(albaranId);
		if (a == null)
			throw new RuntimeException("Albarán no encontrado");

		if (a.getLineas() != null) {
			a.getLineas().removeIf(l -> l != null && l.getId() != null && l.getId().equals(lineaId));
		}

		a.recalcularTotales();
		return albaranRepo.save(a);
	}

	@Transactional
	public AlbaranProveedor confirmar(Long albaranId) {
		AlbaranProveedor a = findById(albaranId);
		if (a == null)
			throw new RuntimeException("Albarán no encontrado");

		a.setConfirmado(true);
		a.recalcularTotales();

		return albaranRepo.save(a);
	}

	private String generarNumeroInterno(String empresa) {
		AlbaranProveedor ultimo = albaranRepo.findTopByEmpresaOrderByIdDesc(empresa);

		int siguiente = 1;
		if (ultimo != null && ultimo.getNumeroInterno() != null && !ultimo.getNumeroInterno().isBlank()) {
			siguiente = extraerSecuencia(ultimo.getNumeroInterno()) + 1;
		}

		String prefijo = normalizarPrefijoEmpresa(empresa);
		return prefijo + "-" + String.format("%05d", siguiente);
	}

	private int extraerSecuencia(String numeroInterno) {
		if (numeroInterno == null || numeroInterno.isBlank())
			return 0;

		String[] partes = numeroInterno.split("-");
		if (partes.length < 2)
			return 0;

		try {
			return Integer.parseInt(partes[1]);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private String normalizarPrefijoEmpresa(String empresa) {
		if (empresa == null || empresa.isBlank())
			return "GEN";

		String valor = empresa.trim().toUpperCase();

		if (valor.contains("ARGASA"))
			return "ARG";
		if (valor.contains("LUGA") || valor.contains("ELECTROLUGA"))
			return "LUG";

		return valor.length() >= 3 ? valor.substring(0, 3) : valor;
	}

	private double safe(Double v) {
		return v != null ? v : 0.0;
	}

	private int safeInt(Integer v, int def) {
		if (v == null)
			return def;
		return v <= 0 ? def : v;
	}
}
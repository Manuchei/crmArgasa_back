package com.empresa.crm.serviceImpl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.empresa.crm.dto.ProveedorDTO;
import com.empresa.crm.entities.Producto;
import com.empresa.crm.entities.Proveedor;
import com.empresa.crm.repositories.ProductoRepository;
import com.empresa.crm.repositories.ProveedorRepository;
import com.empresa.crm.repositories.TrabajoRepository;
import com.empresa.crm.services.ProveedorService;
import com.empresa.crm.tenant.TenantContext;

import jakarta.transaction.Transactional;

@Service
public class ProveedorServiceImpl implements ProveedorService {

	private final ProveedorRepository proveedorRepository;
	private final TrabajoRepository trabajoRepository;
	private final ProductoRepository productoRepository;

	public ProveedorServiceImpl(ProveedorRepository proveedorRepository, TrabajoRepository trabajoRepository,
			ProductoRepository productoRepository) {
		this.proveedorRepository = proveedorRepository;
		this.trabajoRepository = trabajoRepository;
		this.productoRepository = productoRepository;
	}

	@Override
	public List<Proveedor> findAll() {
		String empresa = TenantContext.get();
		List<Proveedor> proveedores = proveedorRepository.findByEmpresa(empresa);

		for (Proveedor proveedor : proveedores) {
			recalcularTotalesProveedor(proveedor, empresa);
		}

		return proveedores;
	}

	@Override
	public Proveedor findById(Long id) {
		String empresa = TenantContext.get();
		Proveedor proveedor = proveedorRepository.findByIdAndEmpresa(id, empresa).orElse(null);

		if (proveedor != null) {
			recalcularTotalesProveedor(proveedor, empresa);
		}

		return proveedor;
	}

	@Override
	public Proveedor save(Proveedor proveedor) {
		String empresa = TenantContext.get();
		proveedor.setEmpresa(empresa);

		if (proveedor.getProductos() == null) {
			proveedor.setProductos(new ArrayList<>());
		}

		for (Producto producto : proveedor.getProductos()) {
			producto.setProveedor(proveedor);
			producto.setEmpresa(empresa);

			if (producto.getCodigo() != null) {
				producto.setCodigo(producto.getCodigo().trim());
			}
			if (producto.getNombre() != null) {
				producto.setNombre(producto.getNombre().trim());
			}
			if (producto.getModelo() != null) {
				producto.setModelo(producto.getModelo().trim());
			}
			if (producto.getPrecioSinIva() == null) {
				producto.setPrecioSinIva(0.0);
			}
		}

		Proveedor guardado = proveedorRepository.save(proveedor);

		recalcularTotalesProveedor(guardado, empresa);

		return proveedorRepository.save(guardado);
	}

	@Override
	public Proveedor saveFromDto(ProveedorDTO proveedorDto) {
		String empresa = TenantContext.get();

		Proveedor proveedor;

		if (proveedorDto.getId() != null) {
			proveedor = proveedorRepository.findByIdAndEmpresa(proveedorDto.getId(), empresa)
					.orElseThrow(() -> new RuntimeException("Proveedor no encontrado con ID: " + proveedorDto.getId()));
		} else {
			proveedor = new Proveedor();
			proveedor.setProductos(new ArrayList<>());
			proveedor.setTrabajos(new ArrayList<>());
			proveedor.setFacturas(new ArrayList<>());
			proveedor.setAlbaranes(new ArrayList<>());
		}

		proveedor.setEmpresa(empresa);
		proveedor.setNombre(trim(proveedorDto.getNombre()));
		proveedor.setOficio(trim(proveedorDto.getOficio()));
		proveedor.setTelefono(trim(proveedorDto.getTelefono()));
		proveedor.setEmail(trim(proveedorDto.getEmail()));
		proveedor.setTrabajaEnArgasa(proveedorDto.isTrabajaEnArgasa());
		proveedor.setTrabajaEnLuga(proveedorDto.isTrabajaEnLuga());
		proveedor.setTrabajoRealizado(trim(proveedorDto.getTrabajoRealizado()));
		proveedor.setDireccion(trim(proveedorDto.getDireccion()));
		proveedor.setCif(trim(proveedorDto.getCif()));
		proveedor.setFechaAltaProveedor(proveedorDto.getFechaAltaProveedor());
		proveedor.setLocalidad(trim(proveedorDto.getLocalidad()));
		proveedor.setCodigoPostal(trim(proveedorDto.getCodigoPostal()));
		proveedor.setProvincia(trim(proveedorDto.getProvincia()));
		proveedor.setPais(trim(proveedorDto.getPais()));
		proveedor.setContacto(trim(proveedorDto.getContacto()));
		proveedor.setDatosBancarios(trim(proveedorDto.getDatosBancarios()));
		proveedor.setNotas(trim(proveedorDto.getNotas()));

		Proveedor guardado = proveedorRepository.save(proveedor);

		recalcularTotalesProveedor(guardado, empresa);

		return proveedorRepository.save(guardado);
	}

	@Override
	@Transactional
	public void deleteById(Long id) {
		String empresa = TenantContext.get();
		proveedorRepository.deleteByIdAndEmpresa(id, empresa);
	}

	@Override
	public List<Proveedor> findByOficio(String oficio) {
		String empresa = TenantContext.get();
		List<Proveedor> proveedores = proveedorRepository.findByEmpresaAndOficio(empresa, oficio);

		for (Proveedor proveedor : proveedores) {
			recalcularTotalesProveedor(proveedor, empresa);
		}

		return proveedores;
	}

	@Override
	public List<Proveedor> findByEmpresa(String empresa) {
		String tenant = TenantContext.get();
		List<Proveedor> proveedores = proveedorRepository.findByEmpresa(tenant);

		for (Proveedor proveedor : proveedores) {
			recalcularTotalesProveedor(proveedor, tenant);
		}

		return proveedores;
	}

	@Override
	public List<Proveedor> buscar(String texto, String empresa, String oficio) {
		String tenant = TenantContext.get();

		List<Proveedor> proveedores = proveedorRepository.buscarAvanzado(texto == null ? "" : texto, tenant,
				oficio == null ? "" : oficio);

		for (Proveedor proveedor : proveedores) {
			recalcularTotalesProveedor(proveedor, tenant);
		}

		return proveedores;
	}

	private void recalcularTotalesProveedor(Proveedor proveedor, String empresa) {
		if (proveedor == null || proveedor.getId() == null || empresa == null || empresa.isBlank()) {
			return;
		}

		double totalTrabajos = safe(trabajoRepository.sumImporteByProveedorIdAndEmpresa(proveedor.getId(), empresa));
		double totalPagadoTrabajos = safe(
				trabajoRepository.sumImportePagadoByProveedorIdAndEmpresa(proveedor.getId(), empresa));

		double totalProductos = 0.0;

		if (proveedor.getProductos() != null) {
			for (Producto producto : proveedor.getProductos()) {
				double precio = safe(producto.getPrecioSinIva());
				double stock = producto.getStock();
				totalProductos += precio * stock;
			}
		}

		double totalCompra = totalTrabajos + totalProductos;
		double totalPagado = totalPagadoTrabajos;
		double pendientePago = totalCompra - totalPagado;

		if (pendientePago < 0) {
			pendientePago = 0;
		}

		proveedor.setImporteTotal(totalCompra);
		proveedor.setImportePagado(totalPagado);
		proveedor.setImportePendiente(pendientePago);
	}

	private double safe(Double value) {
		return value != null ? value : 0.0;
	}

	private String trim(String value) {
		return value == null ? null : value.trim();
	}
}
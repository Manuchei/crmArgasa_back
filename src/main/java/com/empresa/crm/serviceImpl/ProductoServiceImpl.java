package com.empresa.crm.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.empresa.crm.dto.AjusteStockRequest;
import com.empresa.crm.entities.Producto;
import com.empresa.crm.entities.ProductoMovimiento;
import com.empresa.crm.repositories.ProductoMovimientoRepository;
import com.empresa.crm.repositories.ProductoRepository;
import com.empresa.crm.services.ProductoService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository repo;
    private final ProductoMovimientoRepository movimientoRepo;

    @Override
    @Transactional(readOnly = true)
    public List<Producto> listarPorEmpresa(String empresa) {
        validarEmpresa(empresa);
        return repo.findByEmpresa(empresa);
    }

    @Override
    @Transactional
    public Producto crearProducto(Producto producto, String empresa) {
        validarEmpresa(empresa);
        validarProducto(producto);

        String referencia = producto.getReferencia().trim();

        if (repo.findByReferencia(referencia).isPresent()) {
            throw new IllegalArgumentException("Ya existe un producto con esa referencia");
        }

        producto.setFechaAlta(producto.getFechaAlta() != null ? producto.getFechaAlta() : LocalDate.now());
        producto.setReferencia(referencia);
        producto.setMarca(producto.getMarca().trim());
        producto.setModelo(producto.getModelo().trim());
        producto.setFamilia(producto.getFamilia().trim());
        producto.setSubfamilia(producto.getSubfamilia().trim());
        producto.setDescripcion(producto.getDescripcion().trim());
        producto.setUnidades(producto.getUnidades() != null ? producto.getUnidades() : 0);
        producto.setEmpresa(empresa);

        // Referencia interna automática
        producto.setGama(generarGama());

        return repo.save(producto);
    }

    @Override
    @Transactional
    public Producto actualizarProducto(Long id, Producto producto, String empresa) {
        validarEmpresa(empresa);
        validarProducto(producto);

        Producto existente = repo.findByIdAndEmpresa(id, empresa)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado para empresa " + empresa));

        String referencia = producto.getReferencia().trim();

        repo.findByReferencia(referencia).ifPresent(productoConMismaReferencia -> {
            if (!productoConMismaReferencia.getId().equals(id)) {
                throw new IllegalArgumentException("Ya existe otro producto con esa referencia");
            }
        });

        existente.setFechaAlta(producto.getFechaAlta() != null ? producto.getFechaAlta() : existente.getFechaAlta());
        existente.setProveedor(producto.getProveedor());
        existente.setUnidades(producto.getUnidades() != null ? producto.getUnidades() : 0);
        existente.setReferencia(referencia);
        existente.setMarca(producto.getMarca().trim());
        existente.setModelo(producto.getModelo().trim());
        existente.setFamilia(producto.getFamilia().trim());
        existente.setSubfamilia(producto.getSubfamilia().trim());
        existente.setDescripcion(producto.getDescripcion().trim());
        existente.setEmpresa(empresa);

        // No modificamos la gama si ya existe
        if (existente.getGama() == null || existente.getGama().isBlank()) {
            existente.setGama(generarGama());
        }

        return repo.save(existente);
    }

    @Override
    @Transactional
    public Producto ajustarStock(Long id, AjusteStockRequest request, String empresa) {
        validarEmpresa(empresa);

        Producto producto = repo.findByIdAndEmpresa(id, empresa)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado para empresa " + empresa));

        if (request == null || request.getDelta() == null) {
            throw new IllegalArgumentException("Falta 'delta'");
        }

        int delta = request.getDelta();

        if (delta == 0) {
            throw new IllegalArgumentException("'delta' no puede ser 0");
        }

        int unidadesAnteriores = producto.getUnidades() != null ? producto.getUnidades() : 0;
        int unidadesNuevas = unidadesAnteriores + delta;

        if (unidadesNuevas < 0) {
            throw new IllegalArgumentException("Las unidades no pueden quedar en negativo");
        }

        producto.setUnidades(unidadesNuevas);
        Producto productoGuardado = repo.save(producto);

        ProductoMovimiento movimiento = new ProductoMovimiento();
        movimiento.setEmpresa(empresa);
        movimiento.setProducto(productoGuardado);
        movimiento.setTipo(delta > 0 ? "ENTRADA" : "SALIDA");
        movimiento.setCantidad(Math.abs(delta));

        // Mantengo estos nombres porque tu entidad ProductoMovimiento aún usa stockAnterior/stockNuevo
        movimiento.setUnidadesAnteriores(unidadesAnteriores);
        movimiento.setUnidadesNuevas(unidadesNuevas);

        movimiento.setMotivo(
                request.getMotivo() != null && !request.getMotivo().isBlank()
                ? request.getMotivo().trim()
                : null
        );
        movimiento.setFecha(LocalDateTime.now());

        movimientoRepo.save(movimiento);

        return productoGuardado;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoMovimiento> listarMovimientosPorProducto(Long productoId, String empresa) {
        validarEmpresa(empresa);

        repo.findByIdAndEmpresa(productoId, empresa)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado para empresa " + empresa));

        return movimientoRepo.findByEmpresaAndProductoIdOrderByFechaDesc(empresa, productoId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoMovimiento> listarTodosLosMovimientos(String empresa) {
        validarEmpresa(empresa);
        return movimientoRepo.findByEmpresaOrderByFechaDesc(empresa);
    }

    private void validarEmpresa(String empresa) {
        if (empresa == null || empresa.isBlank()) {
            throw new IllegalArgumentException("Empresa no definida");
        }
    }

    private void validarProducto(Producto producto) {
        if (producto == null) {
            throw new IllegalArgumentException("Producto no válido");
        }

        if (producto.getReferencia() == null || producto.getReferencia().isBlank()) {
            throw new IllegalArgumentException("La referencia es obligatoria");
        }

        if (producto.getMarca() == null || producto.getMarca().isBlank()) {
            throw new IllegalArgumentException("La marca es obligatoria");
        }

        if (producto.getModelo() == null || producto.getModelo().isBlank()) {
            throw new IllegalArgumentException("El modelo es obligatorio");
        }

        if (producto.getFamilia() == null || producto.getFamilia().isBlank()) {
            throw new IllegalArgumentException("La familia es obligatoria");
        }

        if (producto.getSubfamilia() == null || producto.getSubfamilia().isBlank()) {
            throw new IllegalArgumentException("La subfamilia es obligatoria");
        }

        if (producto.getDescripcion() == null || producto.getDescripcion().isBlank()) {
            throw new IllegalArgumentException("La descripción es obligatoria");
        }

        if (producto.getUnidades() != null && producto.getUnidades() < 0) {
            throw new IllegalArgumentException("Las unidades no pueden ser negativas");
        }
    }

    private String generarGama() {
        long siguienteNumero = repo.count() + 1;
        return String.format("REF-%06d", siguienteNumero);
    }
}

package com.empresa.crm.services;

import java.util.List;

import com.empresa.crm.dto.AjusteStockRequest;
import com.empresa.crm.entities.Producto;
import com.empresa.crm.entities.ProductoMovimiento;

public interface ProductoService {

	List<Producto> listarPorEmpresa(String empresa);

	Producto crearProducto(Producto producto, String empresa);

	Producto actualizarProducto(Long id, Producto producto, String empresa);

	Producto ajustarStock(Long id, AjusteStockRequest request, String empresa);

	List<ProductoMovimiento> listarMovimientosPorProducto(Long productoId, String empresa);

	List<ProductoMovimiento> listarTodosLosMovimientos(String empresa);
}
package com.empresa.crm.services;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.empresa.crm.entities.Cliente;
import com.empresa.crm.entities.ClienteProducto;
import com.empresa.crm.entities.Producto;
import com.empresa.crm.repositories.ClienteProductoRepository;
import com.empresa.crm.repositories.ClienteRepository;
import com.empresa.crm.repositories.ProductoRepository;

import jakarta.transaction.Transactional;

@Service
public class ClienteProductoService {

    private final ProductoRepository productoRepo;
    private final ClienteRepository clienteRepo;
    private final ClienteProductoRepository cpRepo;

    public ClienteProductoService(ProductoRepository productoRepo,
                                  ClienteRepository clienteRepo,
                                  ClienteProductoRepository cpRepo) {
        this.productoRepo = productoRepo;
        this.clienteRepo = clienteRepo;
        this.cpRepo = cpRepo;
    }

    @Transactional
    public ClienteProducto addProductoToCliente(Long clienteId, Long productoId) {

        // ✅ FIX: estabas buscando el cliente por productoId
        Cliente cliente = clienteRepo.findById(clienteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no existe"));

        Producto producto = productoRepo.findById(productoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no existe"));

        // ✅ (Opcional) Validar empresa si lo necesitas
        // if (cliente.getEmpresa() != null && producto.getEmpresa() != null
        //         && !cliente.getEmpresa().equalsIgnoreCase(producto.getEmpresa())) {
        //     throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El producto no pertenece a la empresa del cliente");
        // }

        // ✅ Baja stock de forma segura (solo si hay stock)
        int updated = productoRepo.decrementStockIfAvailable(productoId);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sin stock disponible");
        }

        // ✅ Crear asignación cliente-producto
        ClienteProducto cp = new ClienteProducto();
        cp.setCliente(cliente);
        cp.setProducto(producto);

        return cpRepo.save(cp);
    }
}

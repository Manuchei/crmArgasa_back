package com.empresa.crm.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.empresa.crm.entities.TrabajoProveedor;

public interface TrabajoProveedorRepository extends JpaRepository<TrabajoProveedor, Long> {
    List<TrabajoProveedor> findByProveedor_Id(Long id);
}

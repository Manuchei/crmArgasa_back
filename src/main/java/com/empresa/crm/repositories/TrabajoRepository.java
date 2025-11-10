package com.empresa.crm.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.empresa.crm.entities.Trabajo;

@Repository
public interface TrabajoRepository extends JpaRepository<Trabajo, Long> {
	List<Trabajo> findByProveedorId(Long proveedorId);

	List<Trabajo> findByPagado(boolean pagado);

    List<Trabajo> findByCliente_Id(Long id);
}

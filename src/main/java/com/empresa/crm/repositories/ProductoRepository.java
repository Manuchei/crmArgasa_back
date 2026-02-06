package com.empresa.crm.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.empresa.crm.entities.Producto;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
	
	Optional<Producto> findByCodigo(String codigo);
	List<Producto> findByEmpresa(String empresa);
	
	//Opcion recomendada: descuento atomico en SQL (evita carreras)
	@Modifying
	@Query("UPDATE Producto p SET p.stock = p.stock -1 WHERE p.id = :id AND p.stock > 0")
	int decrementStockIfAvailable(@Param("id") Long id);

}

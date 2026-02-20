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

	List<Producto> findByEmpresaAndNombreContainingIgnoreCase(String empresa, String nombre);

	// ✅ RESTAR stock (cantidad) si hay stock suficiente
	@Modifying
	@Query("""
			  UPDATE Producto p
			  SET p.stock = p.stock - :cantidad
			  WHERE p.id = :id AND p.stock >= :cantidad
			""")
	int decrementStockIfAvailable(@Param("id") Long id, @Param("cantidad") int cantidad);

	// ✅ SUMAR stock (cantidad)
	@Modifying
	@Query("""
			  UPDATE Producto p
			  SET p.stock = p.stock + :cantidad
			  WHERE p.id = :id
			""")
	int incrementStock(@Param("id") Long id, @Param("cantidad") int cantidad);
}
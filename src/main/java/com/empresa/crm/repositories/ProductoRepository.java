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

	@Modifying
	@Query("""
			UPDATE Producto p
			SET p.stock = p.stock - :cantidad
			WHERE p.id = :id
			  AND UPPER(TRIM(p.empresa)) = UPPER(TRIM(:empresa))
			""")
	int decrementStockIfAvailable(@Param("id") Long id, @Param("cantidad") int cantidad,
			@Param("empresa") String empresa);

	@Modifying
	@Query("""
			UPDATE Producto p
			SET p.stock = p.stock + :cantidad
			WHERE p.id = :id
			  AND UPPER(TRIM(p.empresa)) = UPPER(TRIM(:empresa))
			""")
	int incrementStockByEmpresa(@Param("id") Long id, @Param("cantidad") int cantidad,
			@Param("empresa") String empresa);

	@Modifying
	@Query("""
			UPDATE Producto p
			SET p.stock = p.stock + :cantidad
			WHERE p.id = :id
			""")
	int incrementStock(@Param("id") Long id, @Param("cantidad") int cantidad);

	Optional<Producto> findByIdAndEmpresa(Long id, String empresa);

	@Query("""
			    SELECT COALESCE(SUM(p.precioSinIva * p.stock), 0)
			    FROM Producto p
			    WHERE p.proveedor.id = :proveedorId
			      AND p.empresa = :empresa
			""")
	Double sumTotalByProveedorIdAndEmpresa(@Param("proveedorId") Long proveedorId, @Param("empresa") String empresa);
}
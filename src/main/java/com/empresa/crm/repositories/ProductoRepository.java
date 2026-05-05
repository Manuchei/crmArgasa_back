package com.empresa.crm.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.empresa.crm.entities.Producto;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

	Optional<Producto> findByReferencia(String referencia);

	Optional<Producto> findByGama(String gama);

	List<Producto> findByEmpresa(String empresa);

	List<Producto> findByEmpresaAndDescripcionContainingIgnoreCase(String empresa, String descripcion);

	Optional<Producto> findByIdAndEmpresa(Long id, String empresa);

	@Modifying
	@Query("""
			UPDATE Producto p
			SET p.unidades = p.unidades + :cantidad
			WHERE p.id = :id
			  AND UPPER(TRIM(p.empresa)) = UPPER(TRIM(:empresa))
			""")
	int incrementUnidadesByEmpresa(@Param("id") Long id, @Param("cantidad") int cantidad,
			@Param("empresa") String empresa);

	@Modifying
	@Query("""
			UPDATE Producto p
			SET p.unidades = p.unidades - :cantidad
			WHERE p.id = :id
			  AND UPPER(TRIM(p.empresa)) = UPPER(TRIM(:empresa))
			  AND p.unidades >= :cantidad
			""")
	int decrementUnidadesIfAvailable(@Param("id") Long id, @Param("cantidad") int cantidad,
			@Param("empresa") String empresa);
}
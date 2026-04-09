package com.empresa.crm.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.empresa.crm.entities.Proveedor;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

	List<Proveedor> findByOficio(String oficio);

	List<Proveedor> findByTrabajaEnArgasaTrue();

	List<Proveedor> findByTrabajaEnLugaTrue();

	@Query("SELECT p FROM Proveedor p " + "WHERE LOWER(p.nombre) LIKE LOWER(CONCAT('%', :texto, '%')) "
			+ "   OR LOWER(COALESCE(p.apellido, '')) LIKE LOWER(CONCAT('%', :texto, '%'))")
	List<Proveedor> buscarPorNombreOApellido(@Param("texto") String texto);

	@Query("SELECT p FROM Proveedor p " + "WHERE (LOWER(p.nombre) LIKE LOWER(CONCAT('%', :texto, '%')) "
			+ "    OR LOWER(COALESCE(p.apellido, '')) LIKE LOWER(CONCAT('%', :texto, '%'))) "
			+ "AND LOWER(p.empresa) = LOWER(:empresa)")
	List<Proveedor> buscarPorNombreYEmpresa(@Param("texto") String texto, @Param("empresa") String empresa);

	@Query("""
			    SELECT p FROM Proveedor p
			    WHERE (:texto = '' OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
			           OR LOWER(COALESCE(p.apellido, '')) LIKE LOWER(CONCAT('%', :texto, '%'))
			           OR LOWER(COALESCE(p.contacto, '')) LIKE LOWER(CONCAT('%', :texto, '%'))
			           OR LOWER(COALESCE(p.cif, '')) LIKE LOWER(CONCAT('%', :texto, '%')))
			    AND (:empresa = '' OR LOWER(p.empresa) = LOWER(:empresa))
			    AND (:oficio = '' OR LOWER(COALESCE(p.oficio, '')) = LOWER(:oficio))
			""")
	List<Proveedor> buscarAvanzado(@Param("texto") String texto, @Param("empresa") String empresa,
			@Param("oficio") String oficio);

	List<Proveedor> findByEmpresa(String empresa);

	Optional<Proveedor> findByIdAndEmpresa(Long id, String empresa);

	@Modifying
	@Query("DELETE FROM Proveedor p WHERE p.id = :id AND p.empresa = :empresa")
	void deleteByIdAndEmpresa(@Param("id") Long id, @Param("empresa") String empresa);

	List<Proveedor> findByEmpresaAndOficio(String empresa, String oficio);
}
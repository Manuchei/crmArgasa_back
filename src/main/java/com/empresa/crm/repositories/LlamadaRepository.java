package com.empresa.crm.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.empresa.crm.entities.Llamada;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LlamadaRepository extends JpaRepository<Llamada, Long> {
	List<Llamada> findByFechaBetween(LocalDateTime inicio, LocalDateTime fin);

	List<Llamada> findByEstado(String estado);

	Page<Llamada> findByFechaAfterOrderByFechaAsc(LocalDateTime fecha, Pageable pageable);

	// ✅ Día + empresa
	List<Llamada> findByEmpresaAndFechaBetween(String empresa, LocalDateTime inicio, LocalDateTime fin);

	// ✅ Todos por empresa
	List<Llamada> findByEmpresa(String empresa);

	List<Llamada> findByEmpresaAndEstado(String empresa, String estado);

	Page<Llamada> findByEmpresaAndFechaAfterOrderByFechaAsc(String empresa, LocalDateTime fecha, Pageable pageable);

	Page<Llamada> findByEmpresaAndEstadoAndFechaAfterOrderByFechaAsc(String empresa, String estado, LocalDateTime fecha,
			Pageable pageable);

	List<Llamada> findByEmpresaAndEstadoAndFechaBefore(String empresa, String estado, LocalDateTime fecha);

	List<Llamada> findByEmpresaAndEstadoInAndFechaLessThanEqualOrderByFechaAsc(String empresa, List<String> estados,
			LocalDateTime fecha);

	@Query("""
			    SELECT l
			    FROM Llamada l
			    WHERE l.empresa = :empresa
			      AND LOWER(l.estado) = 'realizada'
			      AND (:nombre IS NULL OR LOWER(l.nombre) LIKE LOWER(CONCAT('%', :nombre, '%')))
			      AND (:direccion IS NULL OR LOWER(l.direccion) LIKE LOWER(CONCAT('%', :direccion, '%')))
			      AND (:inicio IS NULL OR l.fecha >= :inicio)
			      AND (:fin IS NULL OR l.fecha <= :fin)
			    ORDER BY l.fecha DESC
			""")
	List<Llamada> buscarRealizadas(@Param("empresa") String empresa, @Param("nombre") String nombre,
			@Param("direccion") String direccion, @Param("inicio") LocalDateTime inicio,
			@Param("fin") LocalDateTime fin);

}

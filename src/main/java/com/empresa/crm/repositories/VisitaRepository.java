package com.empresa.crm.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.empresa.crm.entities.Visita;

public interface VisitaRepository extends JpaRepository<Visita, Long> {

	List<Visita> findByEmpresa(String empresa);

	List<Visita> findByEmpresaAndFechaBetween(String empresa, LocalDateTime inicio, LocalDateTime fin);

	List<Visita> findByEmpresaAndEstadoAndFechaBefore(String empresa, String estado, LocalDateTime fecha);

	List<Visita> findByEmpresaAndEstadoInAndFechaLessThanEqualOrderByFechaAsc(String empresa, List<String> estados,
			LocalDateTime fecha);

	@Query("""
			SELECT t FROM Tarea t
			WHERE t.empresa = :empresa
			  AND LOWER(t.estado) = 'realizada'
			  AND (:nombre IS NULL OR LOWER(t.nombre) LIKE LOWER(CONCAT('%', :nombre, '%')))
			  AND (:direccion IS NULL OR LOWER(t.direccion) LIKE LOWER(CONCAT('%', :direccion, '%')))
			  AND (:inicio IS NULL OR t.fecha >= :inicio)
			  AND (:fin IS NULL OR t.fecha < :fin)
			ORDER BY t.fecha DESC
			""")
	List<Visita> buscarRealizadas(@Param("empresa") String empresa, @Param("nombre") String nombre,
			@Param("direccion") String direccion, @Param("inicio") LocalDateTime inicio,
			@Param("fin") LocalDateTime fin);
}
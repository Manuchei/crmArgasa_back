package com.empresa.crm.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.empresa.crm.entities.Ruta;

@Repository
public interface RutaRepository extends JpaRepository<Ruta, Long> {

	// legacy
	List<Ruta> findByEstado(String estado);

	List<Ruta> findByNombreTransportistaContainingIgnoreCase(String nombre);

	List<Ruta> findByFecha(LocalDate fecha);

	List<Ruta> findByFechaAndNombreTransportistaIgnoreCase(LocalDate fecha, String nombre);

	@Query("select distinct r.nombreTransportista from Ruta r where r.fecha = :fecha")
	List<String> findTransportistasByFecha(@Param("fecha") LocalDate fecha);

	List<Ruta> findByFechaAndNombreTransportistaContainingIgnoreCase(LocalDate fecha, String nombre);

	// multi-tenant (empresa)
	List<Ruta> findByEmpresa(String empresa);

	List<Ruta> findByEmpresaAndEstado(String empresa, String estado);

	List<Ruta> findByEmpresaAndNombreTransportistaContainingIgnoreCase(String empresa, String nombre);

	List<Ruta> findByEmpresaAndFecha(String empresa, LocalDate fecha);

	List<Ruta> findByEmpresaAndFechaAndNombreTransportistaContainingIgnoreCase(String empresa, LocalDate fecha,
			String nombre);

	@Query("""
			  SELECT r
			  FROM Ruta r
			  LEFT JOIN FETCH r.lineas l
			  LEFT JOIN FETCH l.producto p
			  WHERE r.id = :id
			""")
	Optional<Ruta> findByIdWithLineas(@Param("id") Long id);

	// ✅ para cerrar ruta de forma segura por empresa
	Optional<Ruta> findByIdAndEmpresa(Long id, String empresa);

	// ✅ para cerrar ruta y tener líneas cargadas (si no, puede venir vacío)
	@EntityGraph(attributePaths = { "cliente", "lineas", "lineas.producto" })
	Optional<Ruta> findWithLineasByIdAndEmpresa(Long id, String empresa);
}

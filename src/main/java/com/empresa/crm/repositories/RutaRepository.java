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

	List<Ruta> findByEstado(String estado);

	List<Ruta> findByNombreTransportistaContainingIgnoreCase(String nombre);

	List<Ruta> findByFecha(LocalDate fecha);

	List<Ruta> findByFechaAndNombreTransportistaIgnoreCase(LocalDate fecha, String nombre);

	@Query("""
			  select distinct r.nombreTransportista from Ruta r where r.fecha = :fecha
			""")
	List<String> findTransportistasByFecha(@Param("fecha") LocalDate fecha);

	List<Ruta> findByFechaAndNombreTransportistaContainingIgnoreCase(LocalDate fecha, String nombre);

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

	Optional<Ruta> findByIdAndEmpresa(Long id, String empresa);

	@EntityGraph(attributePaths = { "cliente", "lineas", "lineas.producto" })
	Optional<Ruta> findWithLineasByIdAndEmpresa(Long id, String empresa);

	@Query("""
				select coalesce(sum(l.cantidad), 0)
				from Ruta r
				join r.lineas l
				where r.empresa = :empresa
				  and r.fecha = :fecha
				  and l.producto.id = :productoId
				  and lower(coalesce(r.estado,'')) <> 'cerrada'
			""")
	Integer sumReservadoProductoFecha(@Param("empresa") String empresa, @Param("fecha") LocalDate fecha,
			@Param("productoId") Long productoId);

	@Query("""
				select coalesce(sum(l.cantidad), 0)
				from Ruta r
				join r.lineas l
				where r.empresa = :empresa
				  and r.fecha = :fecha
				  and r.cliente.id = :clienteId
				  and l.producto.id = :productoId
				  and lower(coalesce(r.estado,'')) <> 'cerrada'
			""")
	Integer sumReservadoClienteProductoFecha(@Param("empresa") String empresa, @Param("fecha") LocalDate fecha,
			@Param("clienteId") Long clienteId, @Param("productoId") Long productoId);

	// ✅ MÉTODO VIEJO: lo dejamos para no romper RutaServiceImpl
	@Query("""
				SELECT COALESCE(SUM(rl.cantidad), 0)
				FROM RutaLinea rl
				JOIN rl.ruta r
				WHERE r.empresa = :empresa
				  AND r.cliente.id = :clienteId
				  AND rl.producto.id = :productoId
				  AND LOWER(COALESCE(r.estado, '')) <> 'cerrada'
			""")
	Integer sumReservadoClienteProductoAbierto(@Param("empresa") String empresa, @Param("clienteId") Long clienteId,
			@Param("productoId") Long productoId);

	// ✅ MÉTODO VIEJO: lo dejamos para compatibilidad
	@Query("""
				SELECT COALESCE(SUM(rl.cantidad), 0)
				FROM RutaLinea rl
				JOIN rl.ruta r
				WHERE r.empresa = :empresa
				  AND r.cliente.id = :clienteId
				  AND rl.producto.id = :productoId
				  AND LOWER(COALESCE(r.estado, '')) <> 'cerrada'
				  AND (:excludeRutaId IS NULL OR r.id <> :excludeRutaId)
			""")
	Integer sumReservadoClienteProductoAbiertoExcluyendoRuta(@Param("empresa") String empresa,
			@Param("clienteId") Long clienteId, @Param("productoId") Long productoId,
			@Param("excludeRutaId") Long excludeRutaId);

	// ✅ MÉTODOS NUEVOS: para pendientes por fecha de hoy
	@Query("""
				SELECT COALESCE(SUM(rl.cantidad), 0)
				FROM RutaLinea rl
				JOIN rl.ruta r
				WHERE r.empresa = :empresa
				  AND r.fecha = :fecha
				  AND r.cliente.id = :clienteId
				  AND rl.producto.id = :productoId
				  AND LOWER(COALESCE(r.estado, '')) <> 'cerrada'
			""")
	Integer sumReservadoClienteProductoAbiertoEnFecha(@Param("empresa") String empresa, @Param("fecha") LocalDate fecha,
			@Param("clienteId") Long clienteId, @Param("productoId") Long productoId);

	@Query("""
				SELECT COALESCE(SUM(rl.cantidad), 0)
				FROM RutaLinea rl
				JOIN rl.ruta r
				WHERE r.empresa = :empresa
				  AND r.fecha = :fecha
				  AND r.cliente.id = :clienteId
				  AND rl.producto.id = :productoId
				  AND LOWER(COALESCE(r.estado, '')) <> 'cerrada'
				  AND (:excludeRutaId IS NULL OR r.id <> :excludeRutaId)
			""")
	Integer sumReservadoClienteProductoAbiertoEnFechaExcluyendoRuta(@Param("empresa") String empresa,
			@Param("fecha") LocalDate fecha, @Param("clienteId") Long clienteId, @Param("productoId") Long productoId,
			@Param("excludeRutaId") Long excludeRutaId);
}
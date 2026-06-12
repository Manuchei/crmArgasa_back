package com.empresa.crm.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.empresa.crm.entities.FacturaProveedor;

@Repository
public interface FacturaProveedorRepository extends JpaRepository<FacturaProveedor, Long> {

	List<FacturaProveedor> findByEmpresa(String empresa);

	List<FacturaProveedor> findByPagada(boolean pagada);

	Optional<FacturaProveedor> findByIdAndEmpresa(Long id, String empresa);

	List<FacturaProveedor> findByProveedorIdAndEmpresa(Long proveedorId, String empresa);

	Optional<FacturaProveedor> findTopByEmpresaOrderByIdDesc(String empresa);

	Optional<FacturaProveedor> findByAlbaranProveedorId(Long albaranProveedorId);

	List<FacturaProveedor> findAllByAlbaranProveedorIdAndEmpresa(Long albaranProveedorId, String empresa);

	boolean existsByAlbaranProveedor_IdAndEmpresa(Long albaranProveedorId, String empresa);

	@Query("""
			SELECT f FROM FacturaProveedor f
			WHERE f.empresa = :empresa
			AND (:estado IS NULL OR f.estado = :estado)
			AND (:pagada IS NULL OR f.pagada = :pagada)
			AND (:proveedorId IS NULL OR f.proveedor.id = :proveedorId)
			AND (:desde IS NULL OR f.fechaEmision >= :desde)
			AND (:hasta IS NULL OR f.fechaEmision <= :hasta)
			ORDER BY f.fechaEmision DESC
			""")
	List<FacturaProveedor> buscarInforme(@Param("empresa") String empresa, @Param("estado") String estado,
			@Param("pagada") Boolean pagada, @Param("proveedorId") Long proveedorId, @Param("desde") LocalDate desde,
			@Param("hasta") LocalDate hasta);
}
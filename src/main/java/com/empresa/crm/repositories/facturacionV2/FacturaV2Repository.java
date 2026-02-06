package com.empresa.crm.repositories.facturacionV2;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.empresa.crm.entities.facturacionV2.FacturaV2;

public interface FacturaV2Repository extends JpaRepository<FacturaV2, Long> {

	@EntityGraph(attributePaths = "lineas")
	Optional<FacturaV2> findByIdAndEmpresa(Long id, String empresa);

	List<FacturaV2> findByEmpresaOrderByFechaEmisionDesc(String empresa);

	List<FacturaV2> findByEmpresaAndEstadoOrderByFechaEmisionDesc(String empresa, String estado);

	List<FacturaV2> findByEmpresaAndClienteIdOrderByFechaEmisionDesc(String empresa, Long clienteId);

	List<FacturaV2> findByEmpresaAndClienteIdAndEstadoOrderByFechaEmisionDesc(String empresa, Long clienteId,
			String estado);

	// ✅ filtro por cliente (relación ManyToOne)
	List<FacturaV2> findByEmpresaAndCliente_IdOrderByFechaEmisionDesc(String empresa, Long clienteId);

	List<FacturaV2> findByEmpresaAndCliente_IdAndEstadoOrderByFechaEmisionDesc(String empresa, Long clienteId,
			String estado);

	@Query("""
			  select f
			  from FacturaV2 f
			  join fetch f.cliente c
			  left join fetch f.lineas
			  where f.id = :id and f.empresa = :empresa
			""")
	Optional<FacturaV2> findByIdAndEmpresaWithClienteAndLineas(@Param("id") Long id, @Param("empresa") String empresa);

}

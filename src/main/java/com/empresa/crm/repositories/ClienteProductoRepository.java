package com.empresa.crm.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.empresa.crm.dto.ProductoPendienteDto;
import com.empresa.crm.entities.ClienteProducto;

public interface ClienteProductoRepository extends JpaRepository<ClienteProducto, Long> {

	@Query("""
			select cp
			from ClienteProducto cp
			join cp.cliente c
			join cp.producto p
			where cp.empresa = :empresa
			  and c.id = :clienteId
			  and p.id = :productoId
			  and lower(c.empresa) = lower(:empresa)
			  and lower(p.empresa) = lower(:empresa)
			""")
	Optional<ClienteProducto> findByEmpresaAndClienteIdAndProductoId(@Param("empresa") String empresa,
			@Param("clienteId") Long clienteId, @Param("productoId") Long productoId);

	@Query("""
			select cp
			from ClienteProducto cp
			join cp.cliente c
			where cp.empresa = :empresa
			  and c.id = :clienteId
			  and lower(c.empresa) = lower(:empresa)
			""")
	List<ClienteProducto> findAllByEmpresaAndClienteId(@Param("empresa") String empresa,
			@Param("clienteId") Long clienteId);

	// OJO: delete por @Query necesita @Modifying, pero como ya lo tenías así, lo
	// dejo tal cual.
	// Si no te funciona, te lo corrijo luego (añadiendo @Modifying).
	@Query("""
			delete from ClienteProducto cp
			where cp.empresa = :empresa
			  and cp.cliente.id = :clienteId
			  and cp.producto.id = :productoId
			""")
	void deleteByEmpresaAndClienteIdAndProductoId(@Param("empresa") String empresa, @Param("clienteId") Long clienteId,
			@Param("productoId") Long productoId);

	// ✅ EL BUENO: SOLO PENDIENTES REALES (cantidadTotal - cantidadEntregada) > 0
	@Query("""
			    select new com.empresa.crm.dto.ProductoPendienteDto(
			        p.id,
			        p.codigo,
			        p.nombre,
			        sum( (cp.cantidadTotal - cp.cantidadEntregada) * 1L )
			    )
			    from ClienteProducto cp
			    join cp.producto p
			    where cp.cliente.id = :clienteId
			      and cp.empresa = :empresa
			      and lower(p.empresa) = lower(:empresa)
			    group by p.id, p.codigo, p.nombre
			    having sum( (cp.cantidadTotal - cp.cantidadEntregada) * 1L ) > 0
			""")
	List<ProductoPendienteDto> findPendientesPorCliente(@Param("clienteId") Long clienteId,
			@Param("empresa") String empresa);
}
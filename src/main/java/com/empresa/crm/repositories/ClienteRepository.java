package com.empresa.crm.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.empresa.crm.dto.ClienteResumenDTO;
import com.empresa.crm.entities.Cliente;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

	// ✅ Buscador (FILTRADO por empresa) sin nombreComercial
	// Busca por: nombreApellidos, CIF/DNI, email, teléfono y móvil
	@Query("""
			    SELECT c FROM Cliente c
			    WHERE c.empresa = :empresa AND (
			        LOWER(c.nombreApellidos) LIKE LOWER(CONCAT('%', :texto, '%'))
			        OR LOWER(c.cifDni) LIKE LOWER(CONCAT('%', :texto, '%'))
			        OR LOWER(c.email) LIKE LOWER(CONCAT('%', :texto, '%'))
			        OR LOWER(c.telefono) LIKE LOWER(CONCAT('%', :texto, '%'))
			        OR LOWER(c.movil) LIKE LOWER(CONCAT('%', :texto, '%'))
			    )
			""")
	List<Cliente> buscarPorTexto(@Param("texto") String texto, @Param("empresa") String empresa);

	// Multi-tenant helpers
	List<Cliente> findByEmpresa(String empresa);

	Optional<Cliente> findByIdAndEmpresa(Long id, String empresa);

	void deleteByIdAndEmpresa(Long id, String empresa);

	@Query("""
			    SELECT new com.empresa.crm.dto.ClienteResumenDTO(
			        c.id,
			        c.nombreApellidos,
			        c.telefono,
			        c.movil,
			        c.cifDni,
			        c.email,

			        COALESCE(
			            (SELECT SUM(t.importe)
			             FROM Trabajo t
			             WHERE t.cliente.id = c.id
			             AND t.empresa = :empresa), 0
			        ),

			        (
			            COALESCE(
			                (SELECT SUM(pc.importe)
			                 FROM PagoCliente pc
			                 WHERE pc.cliente.id = c.id
			                 AND pc.empresa = :empresa), 0
			            )
			            +
			            COALESCE(
			                (SELECT SUM(t2.importePagado)
			                 FROM Trabajo t2
			                 WHERE t2.cliente.id = c.id
			                 AND t2.empresa = :empresa), 0
			            )
			        ),

			        (
			            COALESCE(
			                (SELECT SUM(t3.importe)
			                 FROM Trabajo t3
			                 WHERE t3.cliente.id = c.id
			                 AND t3.empresa = :empresa), 0
			            )
			            -
			            (
			                COALESCE(
			                    (SELECT SUM(pc2.importe)
			                     FROM PagoCliente pc2
			                     WHERE pc2.cliente.id = c.id
			                     AND pc2.empresa = :empresa), 0
			                )
			                +
			                COALESCE(
			                    (SELECT SUM(t4.importePagado)
			                     FROM Trabajo t4
			                     WHERE t4.cliente.id = c.id
			                     AND t4.empresa = :empresa), 0
			                )
			            )
			        )
			    )
			    FROM Cliente c
			    WHERE c.empresa = :empresa
			    ORDER BY c.nombreApellidos ASC
			""")
	List<ClienteResumenDTO> findResumenByEmpresa(@Param("empresa") String empresa);
}

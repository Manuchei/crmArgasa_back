package com.empresa.crm.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
    List<Cliente> buscarPorTexto(@Param("texto") String texto,
                                 @Param("empresa") String empresa);

    // Multi-tenant helpers
    List<Cliente> findByEmpresa(String empresa);
    Optional<Cliente> findByIdAndEmpresa(Long id, String empresa);
    void deleteByIdAndEmpresa(Long id, String empresa);
}

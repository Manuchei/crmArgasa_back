package com.empresa.crm.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.empresa.crm.entities.Proveedor;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    // Filtrar por oficio
    List<Proveedor> findByOficio(String oficio);

    // Filtrar solo Argasa
    List<Proveedor> findByTrabajaEnArgasaTrue();

    // Filtrar solo Luga / Mi Electro
    List<Proveedor> findByTrabajaEnLugaTrue();

    // Buscar por nombre o apellido
    @Query("""
        SELECT p FROM Proveedor p
        WHERE LOWER(p.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
           OR LOWER(p.apellido) LIKE LOWER(CONCAT('%', :texto, '%'))
    """)
    List<Proveedor> buscarPorNombreOApellido(@Param("texto") String texto);

    // Buscar por nombre y empresa (Argasa o Luga)
    @Query("""
        SELECT p FROM Proveedor p
        WHERE (LOWER(p.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(p.apellido) LIKE LOWER(CONCAT('%', :texto, '%')))
        AND (
               (:empresa = 'argasa' AND p.trabajaEnArgasa = true)
            OR (:empresa = 'luga'   AND p.trabajaEnLuga   = true)
        )
    """)
    List<Proveedor> buscarPorNombreYEmpresa(
            @Param("texto") String texto,
            @Param("empresa") String empresa
    );
}

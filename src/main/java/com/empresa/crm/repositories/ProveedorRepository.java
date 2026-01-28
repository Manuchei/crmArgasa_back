package com.empresa.crm.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.empresa.crm.entities.Proveedor;
import java.util.Optional;


public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    // -------------------------
    //  BÚSQUEDAS SENCILLAS
    // -------------------------
    List<Proveedor> findByOficio(String oficio);

    List<Proveedor> findByTrabajaEnArgasaTrue();

    List<Proveedor> findByTrabajaEnLugaTrue();


    // ----------------------------------------------
    //  BÚSQUEDA POR NOMBRE O APELLIDO (solo texto)
    // ----------------------------------------------
    @Query("SELECT p FROM Proveedor p " +
           "WHERE LOWER(p.nombre) LIKE LOWER(CONCAT('%', :texto, '%')) " +
           "   OR LOWER(p.apellido) LIKE LOWER(CONCAT('%', :texto, '%'))")
    List<Proveedor> buscarPorNombreOApellido(@Param("texto") String texto);


    // ------------------------------------------------------
    //  BÚSQUEDA POR NOMBRE + EMPRESA
    // ------------------------------------------------------
    @Query("SELECT p FROM Proveedor p " +
           "WHERE (LOWER(p.nombre) LIKE LOWER(CONCAT('%', :texto, '%')) " +
           "    OR LOWER(p.apellido) LIKE LOWER(CONCAT('%', :texto, '%'))) " +
           "AND LOWER(p.empresa) = LOWER(:empresa)")
    List<Proveedor> buscarPorNombreYEmpresa(
            @Param("texto") String texto,
            @Param("empresa") String empresa);


    // ------------------------------------------------------
    //  BÚSQUEDA AVANZADA (texto + empresa + oficio)
    // ------------------------------------------------------
    @Query("""
        SELECT p FROM Proveedor p
        WHERE (:texto = '' OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :texto, '%')) 
               OR LOWER(p.apellido) LIKE LOWER(CONCAT('%', :texto, '%')))
        AND (:empresa = '' OR LOWER(p.empresa) = LOWER(:empresa))
        AND (:oficio = '' OR LOWER(p.oficio) = LOWER(:oficio))
    """)
    List<Proveedor> buscarAvanzado(
            @Param("texto") String texto,
            @Param("empresa") String empresa,
            @Param("oficio") String oficio);
    

    List<Proveedor> findByEmpresa(String empresa);
    Optional<Proveedor> findByIdAndEmpresa(Long id, String empresa);
    void deleteByIdAndEmpresa(Long id, String empresa);

    List<Proveedor> findByEmpresaAndOficio(String empresa, String oficio);

}

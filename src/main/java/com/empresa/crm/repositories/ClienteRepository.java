package com.empresa.crm.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.empresa.crm.entities.Cliente;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    // ✅ Buscar por nombre/apellidos, nombre comercial o CIF/DNI (insensible a mayúsculas)
    @Query("SELECT c FROM Cliente c " +
           "WHERE LOWER(c.nombreApellidos) LIKE LOWER(CONCAT('%', :texto, '%')) " +
           "OR LOWER(c.nombreComercial) LIKE LOWER(CONCAT('%', :texto, '%')) " +
           "OR LOWER(c.cifDni) LIKE LOWER(CONCAT('%', :texto, '%'))")
    List<Cliente> buscarPorTexto(@Param("texto") String texto);

    // ✅ Buscar por texto y filtrando por nombre comercial
    @Query("SELECT c FROM Cliente c " +
           "WHERE (LOWER(c.nombreApellidos) LIKE LOWER(CONCAT('%', :texto, '%')) " +
           "OR LOWER(c.nombreComercial) LIKE LOWER(CONCAT('%', :texto, '%')) " +
           "OR LOWER(c.cifDni) LIKE LOWER(CONCAT('%', :texto, '%'))) " +
           "AND LOWER(c.nombreComercial) = LOWER(:nombreComercial)")
    List<Cliente> buscarPorTextoYNombreComercial(@Param("texto") String texto,
                                                 @Param("nombreComercial") String nombreComercial);
}

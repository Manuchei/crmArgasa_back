package com.empresa.crm.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.empresa.crm.entities.Cliente;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    // 🔹 Buscar por nombre o apellido (insensible a mayúsculas)
    @Query("SELECT c FROM Cliente c " +
           "WHERE LOWER(c.nombre) LIKE LOWER(CONCAT('%', :texto, '%')) " +
           "OR LOWER(c.apellido) LIKE LOWER(CONCAT('%', :texto, '%'))")
    List<Cliente> buscarPorNombreOApellido(@Param("texto") String texto);

    // 🔹 Buscar también filtrando por empresa
    @Query("SELECT c FROM Cliente c " +
           "WHERE (LOWER(c.nombre) LIKE LOWER(CONCAT('%', :texto, '%')) " +
           "OR LOWER(c.apellido) LIKE LOWER(CONCAT('%', :texto, '%'))) " +
           "AND LOWER(c.empresa) = LOWER(:empresa)")
    List<Cliente> buscarPorNombreYEmpresa(@Param("texto") String texto, @Param("empresa") String empresa);
}
package com.empresa.crm.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.empresa.crm.entities.Proveedor;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
    List<Proveedor> findByOficio(String oficio);
    List<Proveedor> findByTrabajaEnArgasaTrue();
    List<Proveedor> findByTrabajaEnLugaTrue();
    
    @Query("SELECT p FROM Proveedor p " +
            "WHERE LOWER(p.nombre) LIKE LOWER(CONCAT('%', :texto, '%')) " +
            "OR LOWER(p.apellido) LIKE LOWER(CONCAT('%', :texto, '%'))")
     List<Proveedor> buscarPorNombreOApellido(@Param("texto") String texto);

     @Query("SELECT p FROM Proveedor p " +
            "WHERE (LOWER(p.nombre) LIKE LOWER(CONCAT('%', :texto, '%')) " +
            "OR LOWER(p.apellido) LIKE LOWER(CONCAT('%', :texto, '%'))) " +
            "AND ((:empresa = 'argasa' AND p.trabajaEnArgasa = true) " +
            "OR (:empresa = 'luga' AND p.trabajaEnLuga = true))")
		List<Proveedor> buscarPorNombreYEmpresa(@Param("texto") String texto, @Param("empresa") String empresa);
 
}
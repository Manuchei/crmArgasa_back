package com.empresa.crm.repositories.facturacionV2;


import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import com.empresa.crm.entities.facturacionV2.ContadorFacturaV2;

public interface ContadorFacturaV2Repository extends JpaRepository<ContadorFacturaV2, Long> {
  Optional<ContadorFacturaV2> findByEmpresaAndSerie(String empresa, String serie);
}

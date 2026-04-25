package br.com.cmms.cmms.repository;

import br.com.cmms.cmms.model.Maquina;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaquinaRepository extends JpaRepository<Maquina, Long> {
    long countByStatus(String status);
    List<Maquina> findByIntervaloPreventivaDiasGreaterThan(int dias);
}

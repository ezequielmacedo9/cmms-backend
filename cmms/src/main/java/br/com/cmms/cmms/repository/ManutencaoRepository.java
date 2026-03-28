package br.com.cmms.cmms.repository;

import br.com.cmms.cmms.model.Manutencao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ManutencaoRepository extends JpaRepository<Manutencao, Long> {
    List<Manutencao> findByMaquinaIdOrderByDataManutencaoDesc(Long maquinaId);
}

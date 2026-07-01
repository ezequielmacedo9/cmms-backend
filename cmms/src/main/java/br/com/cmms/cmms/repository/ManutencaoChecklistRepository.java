package br.com.cmms.cmms.repository;

import br.com.cmms.cmms.model.ManutencaoChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ManutencaoChecklistRepository extends JpaRepository<ManutencaoChecklistItem, Long> {
    List<ManutencaoChecklistItem> findByManutencaoIdOrderById(Long manutencaoId);
    Optional<ManutencaoChecklistItem> findByIdAndEmpresaId(Long id, Long empresaId);
}

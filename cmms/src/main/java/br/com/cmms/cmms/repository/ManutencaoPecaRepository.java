package br.com.cmms.cmms.repository;

import br.com.cmms.cmms.model.ManutencaoPeca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ManutencaoPecaRepository extends JpaRepository<ManutencaoPeca, Long> {

    List<ManutencaoPeca> findByManutencaoId(Long manutencaoId);

    List<ManutencaoPeca> findByManutencaoIdAndEmpresaId(Long manutencaoId, Long empresaId);

    /** Total parts cost across all work orders of one empresa (for the cost KPI). */
    @Query("SELECT COALESCE(SUM(mp.quantidade * mp.custoUnitario), 0) FROM ManutencaoPeca mp WHERE mp.empresaId = :empresaId")
    double sumCustoByEmpresa(@Param("empresaId") Long empresaId);
}

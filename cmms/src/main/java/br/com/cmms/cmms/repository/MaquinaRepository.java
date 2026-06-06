package br.com.cmms.cmms.repository;

import br.com.cmms.cmms.model.Maquina;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaquinaRepository extends JpaRepository<Maquina, Long> {

    /** Tenant-scoped fetch by id — the building block that closes IDOR on reads/writes. */
    Optional<Maquina> findByIdAndEmpresaId(Long id, Long empresaId);

    /** Tenant-scoped full list (reports / legacy non-paged callers). */
    List<Maquina> findByEmpresaId(Long empresaId);

    /** Cross-tenant scan used only by the system scheduler (logs overdue preventives). */
    List<Maquina> findByIntervaloPreventivaDiasGreaterThan(int dias);

    /**
     * Free-text search across nome and setor, optionally constrained by status,
     * always constrained to the caller's empresa.
     */
    @Query("""
        SELECT m FROM Maquina m
        WHERE m.empresaId = :empresaId
          AND (:q IS NULL OR LOWER(m.nome)  LIKE LOWER(CONCAT('%', :q, '%'))
                          OR LOWER(m.setor) LIKE LOWER(CONCAT('%', :q, '%')))
          AND (:status IS NULL OR m.status = :status)
    """)
    Page<Maquina> search(@Param("q") String q,
                         @Param("status") String status,
                         @Param("empresaId") Long empresaId,
                         Pageable pageable);

    /** Aggregate count by status for one empresa. Returns {@code [status, count]} rows. */
    @Query("""
        SELECT m.status AS status, COUNT(m) AS total
        FROM Maquina m
        WHERE m.empresaId = :empresaId
        GROUP BY m.status
    """)
    List<Object[]> countGroupByStatus(@Param("empresaId") Long empresaId);

    /** Lightweight projections for overdue-preventive alerts, scoped to one empresa. */
    @Query("""
        SELECT m.id, m.nome, m.setor, m.prioridade,
               m.dataUltimaManutencao, m.intervaloPreventivaDias
        FROM Maquina m
        WHERE m.empresaId = :empresaId
          AND m.intervaloPreventivaDias IS NOT NULL
          AND m.intervaloPreventivaDias > 0
    """)
    List<Object[]> findPreventiveCandidates(@Param("empresaId") Long empresaId);
}

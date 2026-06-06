package br.com.cmms.cmms.repository;

import br.com.cmms.cmms.model.Manutencao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * {@code @EntityGraph(attributePaths = "maquina")} forces Hibernate to fetch
 * the associated machine in the same query, eliminating the N+1 that hits
 * the API every time a list of maintenances is serialised to DTO.
 *
 * <p>Every read is scoped to the caller's empresa via {@code empresaId}.
 */
public interface ManutencaoRepository extends JpaRepository<Manutencao, Long> {

    /** Tenant-scoped fetch by id — closes IDOR on read/delete. */
    @EntityGraph(attributePaths = "maquina")
    Optional<Manutencao> findByIdAndEmpresaId(Long id, Long empresaId);

    /** Paged listing — ordering comes from the request Pageable (default dataManutencao DESC). */
    @EntityGraph(attributePaths = "maquina")
    Page<Manutencao> findByEmpresaId(Long empresaId, Pageable pageable);

    /** Non-paged listing for reports / legacy callers. */
    @EntityGraph(attributePaths = "maquina")
    List<Manutencao> findByEmpresaIdOrderByDataManutencaoDesc(Long empresaId);

    @EntityGraph(attributePaths = "maquina")
    Page<Manutencao> findByMaquinaIdAndEmpresaIdOrderByDataManutencaoDesc(Long maquinaId, Long empresaId, Pageable pageable);

    @EntityGraph(attributePaths = "maquina")
    List<Manutencao> findByMaquinaIdAndEmpresaIdOrderByDataManutencaoDesc(Long maquinaId, Long empresaId);

    /** Aggregate count by maintenance type for one empresa. */
    @Query("""
        SELECT m.tipo AS tipo, COUNT(m) AS total
        FROM Manutencao m
        WHERE m.empresaId = :empresaId
        GROUP BY m.tipo
    """)
    List<Object[]> countGroupByTipo(@Param("empresaId") Long empresaId);

    /**
     * Lightweight rows for MTBF computation (corrective maintenances), scoped
     * to one empresa: {@code [machineId, date]} ordered for a linear scan.
     */
    @Query("""
        SELECT m.maquina.id, m.dataManutencao
        FROM Manutencao m
        WHERE m.empresaId = :empresaId
          AND m.tipo = 'CORRETIVA'
          AND m.dataManutencao IS NOT NULL
          AND m.maquina IS NOT NULL
        ORDER BY m.maquina.id, m.dataManutencao
    """)
    List<Object[]> findCorrectiveDatesPerMachine(@Param("empresaId") Long empresaId);

    /**
     * Monthly histogram of maintenance counts since the given start date for
     * one empresa. Returns rows of {@code [year, month, count]}.
     */
    @Query("""
        SELECT EXTRACT(YEAR  FROM m.dataManutencao) AS year,
               EXTRACT(MONTH FROM m.dataManutencao) AS month,
               COUNT(m) AS total
        FROM Manutencao m
        WHERE m.empresaId = :empresaId
          AND m.dataManutencao IS NOT NULL
          AND m.dataManutencao >= :start
        GROUP BY EXTRACT(YEAR FROM m.dataManutencao),
                 EXTRACT(MONTH FROM m.dataManutencao)
    """)
    List<Object[]> monthlyCountsSince(@Param("start") LocalDate start, @Param("empresaId") Long empresaId);
}

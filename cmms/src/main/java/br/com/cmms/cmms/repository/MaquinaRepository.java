package br.com.cmms.cmms.repository;

import br.com.cmms.cmms.model.Maquina;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaquinaRepository extends JpaRepository<Maquina, Long> {

    long countByStatus(String status);

    List<Maquina> findByIntervaloPreventivaDiasGreaterThan(int dias);

    /**
     * Free-text search across nome and setor, optionally constrained by status.
     * {@code :q} and {@code :status} are nullable — pass {@code null} to skip.
     */
    @Query("""
        SELECT m FROM Maquina m
        WHERE (:q IS NULL OR LOWER(m.nome)  LIKE LOWER(CONCAT('%', :q, '%'))
                          OR LOWER(m.setor) LIKE LOWER(CONCAT('%', :q, '%')))
          AND (:status IS NULL OR m.status = :status)
    """)
    Page<Maquina> search(@Param("q") String q,
                         @Param("status") String status,
                         Pageable pageable);

    /**
     * Aggregate count by status, returned as {@code [status, count]} rows
     * so the dashboard service can map directly without loading all machines.
     */
    @Query("SELECT m.status AS status, COUNT(m) AS total FROM Maquina m GROUP BY m.status")
    List<Object[]> countGroupByStatus();

    /**
     * Returns lightweight projections for the overdue-preventive alerts:
     * id, nome, setor, prioridade, dataUltimaManutencao, intervaloPreventivaDias.
     * Excludes anything we don't need so Hibernate can avoid loading the entity.
     */
    @Query("""
        SELECT m.id, m.nome, m.setor, m.prioridade,
               m.dataUltimaManutencao, m.intervaloPreventivaDias
        FROM Maquina m
        WHERE m.intervaloPreventivaDias IS NOT NULL
          AND m.intervaloPreventivaDias > 0
    """)
    List<Object[]> findPreventiveCandidates();
}

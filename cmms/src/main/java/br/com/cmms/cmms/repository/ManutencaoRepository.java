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

/**
 * {@code @EntityGraph(attributePaths = "maquina")} forces Hibernate to fetch
 * the associated machine in the same query, eliminating the N+1 that hits
 * the API every time a list of maintenances is serialised to DTO.
 */
public interface ManutencaoRepository extends JpaRepository<Manutencao, Long> {

    @Override
    @EntityGraph(attributePaths = "maquina")
    Page<Manutencao> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "maquina")
    List<Manutencao> findAll();

    @EntityGraph(attributePaths = "maquina")
    Page<Manutencao> findByMaquinaIdOrderByDataManutencaoDesc(Long maquinaId, Pageable pageable);

    /** Kept for the scheduler / non-paged callers. */
    @EntityGraph(attributePaths = "maquina")
    List<Manutencao> findByMaquinaIdOrderByDataManutencaoDesc(Long maquinaId);

    @EntityGraph(attributePaths = "maquina")
    List<Manutencao> findByDataManutencaoBetween(LocalDate start, LocalDate end);

    @Query("SELECT m FROM Manutencao m JOIN FETCH m.maquina WHERE m.tipo = :tipo ORDER BY m.maquina.id ASC, m.dataManutencao ASC")
    List<Manutencao> findByTipoOrderByMaquinaAndDate(@Param("tipo") String tipo);
}

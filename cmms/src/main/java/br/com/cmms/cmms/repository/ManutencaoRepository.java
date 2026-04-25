package br.com.cmms.cmms.repository;

import br.com.cmms.cmms.model.Manutencao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ManutencaoRepository extends JpaRepository<Manutencao, Long> {
    List<Manutencao> findByMaquinaIdOrderByDataManutencaoDesc(Long maquinaId);

    List<Manutencao> findByDataManutencaoBetween(LocalDate start, LocalDate end);

    @Query("SELECT m FROM Manutencao m WHERE m.tipo = :tipo ORDER BY m.maquina.id ASC, m.dataManutencao ASC")
    List<Manutencao> findByTipoOrderByMaquinaAndDate(@Param("tipo") String tipo);
}

package br.com.cmms.cmms.repository;

import br.com.cmms.cmms.model.ConfiguracaoSistema;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConfiguracaoSistemaRepository extends JpaRepository<ConfiguracaoSistema, String> {

    List<ConfiguracaoSistema> findByGrupo(String grupo);

    /**
     * Free-text search over {@code chave} and {@code descricao} with optional
     * {@code grupo} filter. Both inputs accept {@code null} to skip the
     * corresponding predicate.
     */
    @Query("""
        SELECT c FROM ConfiguracaoSistema c
        WHERE (:q     IS NULL OR LOWER(c.chave)     LIKE LOWER(CONCAT('%', :q, '%'))
                              OR LOWER(c.descricao) LIKE LOWER(CONCAT('%', :q, '%')))
          AND (:grupo IS NULL OR c.grupo = :grupo)
    """)
    Page<ConfiguracaoSistema> search(@Param("q") String q,
                                     @Param("grupo") String grupo,
                                     Pageable pageable);
}

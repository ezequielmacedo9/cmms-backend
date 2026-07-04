package br.com.cmms.cmms.repository;

import br.com.cmms.cmms.model.ConfiguracaoSistema;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConfiguracaoSistemaRepository extends JpaRepository<ConfiguracaoSistema, Long> {

    List<ConfiguracaoSistema> findByEmpresaId(Long empresaId);

    Optional<ConfiguracaoSistema> findByEmpresaIdAndChave(Long empresaId, String chave);

    boolean existsByEmpresaIdAndChave(Long empresaId, String chave);

    @Query("""
        SELECT c FROM ConfiguracaoSistema c
        WHERE c.empresaId = :empresaId
          AND (:q     IS NULL OR LOWER(c.chave)     LIKE LOWER(CONCAT('%', :q, '%'))
                              OR LOWER(c.descricao) LIKE LOWER(CONCAT('%', :q, '%')))
          AND (:grupo IS NULL OR c.grupo = :grupo)
    """)
    Page<ConfiguracaoSistema> search(@Param("q") String q,
                                     @Param("grupo") String grupo,
                                     @Param("empresaId") Long empresaId,
                                     Pageable pageable);
}

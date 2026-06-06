package br.com.cmms.cmms.repository;

import br.com.cmms.cmms.model.Peca;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PecaRepository extends JpaRepository<Peca, Long> {

    Optional<Peca> findByIdAndEmpresaId(Long id, Long empresaId);

    List<Peca> findByEmpresaId(Long empresaId);

    long countByEmpresaId(Long empresaId);

    @Query("""
        SELECT p FROM Peca p
        WHERE p.empresaId = :empresaId
          AND (:q IS NULL
               OR LOWER(p.nome)   LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(p.codigo) LIKE LOWER(CONCAT('%', :q, '%')))
    """)
    Page<Peca> search(@Param("q") String q, @Param("empresaId") Long empresaId, Pageable pageable);
}

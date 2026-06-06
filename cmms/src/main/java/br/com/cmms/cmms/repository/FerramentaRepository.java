package br.com.cmms.cmms.repository;

import br.com.cmms.cmms.model.Ferramenta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FerramentaRepository extends JpaRepository<Ferramenta, Long> {

    Optional<Ferramenta> findByIdAndEmpresaId(Long id, Long empresaId);

    List<Ferramenta> findByEmpresaId(Long empresaId);

    @Query("""
        SELECT f FROM Ferramenta f
        WHERE f.empresaId = :empresaId
          AND (:q IS NULL
               OR LOWER(f.nome)   LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(f.codigo) LIKE LOWER(CONCAT('%', :q, '%')))
    """)
    Page<Ferramenta> search(@Param("q") String q, @Param("empresaId") Long empresaId, Pageable pageable);
}

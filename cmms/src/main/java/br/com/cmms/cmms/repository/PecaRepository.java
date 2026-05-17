package br.com.cmms.cmms.repository;

import br.com.cmms.cmms.model.Peca;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PecaRepository extends JpaRepository<Peca, Long> {

    @Query("""
        SELECT p FROM Peca p
        WHERE :q IS NULL
           OR LOWER(p.nome)   LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(p.codigo) LIKE LOWER(CONCAT('%', :q, '%'))
    """)
    Page<Peca> search(@Param("q") String q, Pageable pageable);
}

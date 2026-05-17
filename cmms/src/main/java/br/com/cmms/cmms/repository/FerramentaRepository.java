package br.com.cmms.cmms.repository;

import br.com.cmms.cmms.model.Ferramenta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FerramentaRepository extends JpaRepository<Ferramenta, Long> {

    @Query("""
        SELECT f FROM Ferramenta f
        WHERE :q IS NULL
           OR LOWER(f.nome)   LIKE LOWER(CONCAT('%', :q, '%'))
           OR LOWER(f.codigo) LIKE LOWER(CONCAT('%', :q, '%'))
    """)
    Page<Ferramenta> search(@Param("q") String q, Pageable pageable);
}

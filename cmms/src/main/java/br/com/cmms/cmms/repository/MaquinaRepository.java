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
}

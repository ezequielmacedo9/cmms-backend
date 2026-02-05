package br.com.cmms.cmms.repository;

import br.com.cmms.cmms.model.Ferramenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FerramentaRepository extends JpaRepository<Ferramenta, Long> {
}

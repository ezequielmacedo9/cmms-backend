package br.com.cmms.cmms.repository;

import br.com.cmms.cmms.model.Ferramenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FerramentaRepository extends JpaRepository<Ferramenta, Long> {
    List<Ferramenta> findAllByEmpresaId(Long empresaId);
}

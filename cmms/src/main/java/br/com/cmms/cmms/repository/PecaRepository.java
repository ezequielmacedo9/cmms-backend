package br.com.cmms.cmms.repository;

import br.com.cmms.cmms.model.Peca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PecaRepository extends JpaRepository<Peca, Long> {
    List<Peca> findAllByEmpresaId(Long empresaId);
}

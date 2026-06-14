package br.com.cmms.cmms.repository;

import br.com.cmms.cmms.model.Assinatura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AssinaturaRepository extends JpaRepository<Assinatura, Long> {
    Optional<Assinatura> findByEmpresaId(Long empresaId);
}

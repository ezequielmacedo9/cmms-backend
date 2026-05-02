package br.com.cmms.cmms.repository;

import br.com.cmms.cmms.model.Assinatura;
import br.com.cmms.cmms.model.StatusAssinatura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AssinaturaRepository extends JpaRepository<Assinatura, Long> {
    Optional<Assinatura> findByEmpresaId(Long empresaId);
    List<Assinatura> findByStatusAndDataProximaCobrancaBefore(StatusAssinatura status, LocalDate date);
    Optional<Assinatura> findByGatewayAssinaturaId(String gatewayAssinaturaId);
}

package br.com.cmms.cmms.repository;

import br.com.cmms.cmms.model.ConfiguracaoSistema;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ConfiguracaoSistemaRepository extends JpaRepository<ConfiguracaoSistema, String> {
    List<ConfiguracaoSistema> findByGrupo(String grupo);
}

package br.com.cmms.cmms.repository;

import br.com.cmms.cmms.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
    List<Usuario> findAllByOrderByDataCriacaoDesc();
    List<Usuario> findAllByEmpresaIdOrderByDataCriacaoDesc(Long empresaId);
    long countByEmpresaId(Long empresaId);
}

package br.com.cmms.cmms.repository;

import br.com.cmms.cmms.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Non-paged variant — used by callers that genuinely need every user (rare). */
    @EntityGraph(attributePaths = "role")
    List<Usuario> findAllByOrderByDataCriacaoDesc();

    /** Paged listing, eagerly loading the role to avoid N+1 in the UI grid. */
    @EntityGraph(attributePaths = "role")
    Page<Usuario> findAllByOrderByDataCriacaoDesc(Pageable pageable);
}

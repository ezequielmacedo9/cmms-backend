package br.com.cmms.cmms.repository;

import br.com.cmms.cmms.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Auth lookups are intentionally GLOBAL — email is unique across the whole
    // platform, and the tenant is derived FROM the matched user at login.
    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Tenant-scoped fetch by id — closes IDOR on user management. */
    @EntityGraph(attributePaths = "role")
    Optional<Usuario> findByIdAndEmpresaId(Long id, Long empresaId);

    /** Active (non-deleted) user count for plan-quota enforcement. */
    long countByEmpresaId(Long empresaId);

    /** Tenant-scoped non-paged listing. */
    @EntityGraph(attributePaths = "role")
    List<Usuario> findByEmpresaIdOrderByDataCriacaoDesc(Long empresaId);

    /** Tenant-scoped paged listing, eagerly loading the role to avoid N+1 in the UI grid. */
    @EntityGraph(attributePaths = "role")
    Page<Usuario> findByEmpresaIdOrderByDataCriacaoDesc(Long empresaId, Pageable pageable);
}

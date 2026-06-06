package br.com.cmms.cmms.security;

import br.com.cmms.cmms.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Resolves the tenant (empresa) of the currently authenticated user.
 *
 * <p>Multi-tenancy in this application is row-based: every operational row
 * carries an {@code empresa_id} and every read/write is explicitly scoped to
 * the caller's empresa. This bean is the single source of truth for "which
 * empresa is the current request acting on", derived from the authenticated
 * {@link UserDetailsImpl} principal.
 *
 * <p>Exposed as an injectable bean (rather than a static helper) so services
 * stay trivially unit-testable — tests simply mock {@code requireEmpresaId()}.
 */
@Component
public class TenantResolver {

    /** Returns the current empresa id, or {@code null} when there is no tenant-bound principal. */
    public Long empresaIdAtual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl ud) {
            return ud.getUsuario().getEmpresaId();
        }
        return null;
    }

    /**
     * Returns the current empresa id or fails. Use on every tenant-scoped
     * read/write so a missing tenant is a hard error rather than a silent
     * cross-tenant query.
     */
    public Long requireEmpresaId() {
        Long id = empresaIdAtual();
        if (id == null) {
            throw new UnauthorizedException("NO_TENANT",
                "Empresa do usuário não resolvida. Faça login novamente.");
        }
        return id;
    }
}

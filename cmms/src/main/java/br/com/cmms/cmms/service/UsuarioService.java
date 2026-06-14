package br.com.cmms.cmms.service;

import br.com.cmms.cmms.dto.AlterarRoleRequestDTO;
import br.com.cmms.cmms.dto.ConvidarUsuarioRequestDTO;
import br.com.cmms.cmms.dto.UsuarioResponseDTO;
import br.com.cmms.cmms.exception.ConflictException;
import br.com.cmms.cmms.exception.ForbiddenException;
import br.com.cmms.cmms.exception.NotFoundException;
import br.com.cmms.cmms.exception.ValidationException;
import br.com.cmms.cmms.model.Role;
import br.com.cmms.cmms.model.Usuario;
import br.com.cmms.cmms.repository.RoleRepository;
import br.com.cmms.cmms.repository.UsuarioRepository;
import br.com.cmms.cmms.security.TenantResolver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * User management, scoped to the operator's empresa. The operator can only see
 * and manage users that belong to the same empresa (target lookups go through
 * {@code findByIdAndEmpresaId} — closing IDOR across tenants).
 */
@Service
public class UsuarioService {

    private static final Set<String> ROLES_VALIDAS = Set.of(
        "ROLE_SUPER_ADMIN", "ROLE_ADMIN", "ROLE_GESTOR", "ROLE_TECNICO", "ROLE_VISUALIZADOR"
    );

    private final UsuarioRepository usuarioRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;
    private final TenantResolver tenant;
    private final AssinaturaService assinatura;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          RoleRepository roleRepository,
                          PasswordEncoder passwordEncoder,
                          AuditService audit,
                          TenantResolver tenant,
                          AssinaturaService assinatura) {
        this.usuarioRepository = usuarioRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
        this.tenant = tenant;
        this.assinatura = assinatura;
    }

    public List<UsuarioResponseDTO> listar() {
        return usuarioRepository.findByEmpresaIdOrderByDataCriacaoDesc(tenant.requireEmpresaId())
            .stream()
            .map(UsuarioResponseDTO::from)
            .toList();
    }

    public Page<UsuarioResponseDTO> listar(Pageable pageable) {
        return usuarioRepository.findByEmpresaIdOrderByDataCriacaoDesc(tenant.requireEmpresaId(), pageable)
            .map(UsuarioResponseDTO::from);
    }

    public UsuarioResponseDTO getMeuPerfil(String email) {
        Usuario u = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new NotFoundException("USUARIO_NOT_FOUND", "Usuário não encontrado."));
        return UsuarioResponseDTO.from(u);
    }

    public UsuarioResponseDTO convidar(ConvidarUsuarioRequestDTO dto, String emailOperador) {
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new ConflictException("EMAIL_ALREADY_REGISTERED", "Email já cadastrado.");
        }
        if (!ROLES_VALIDAS.contains(dto.getRoleNome())) {
            throw new ValidationException("INVALID_ROLE", "Role inválida: " + dto.getRoleNome());
        }

        Usuario operador = findByEmailOrThrow(emailOperador);
        validarPermissaoRole(operador, dto.getRoleNome());
        assinatura.assertPodeCriarUsuario(operador.getEmpresaId()); // plan-quota gate

        Role role = roleRepository.findByNome(dto.getRoleNome())
            .orElseThrow(() -> new NotFoundException("ROLE_NOT_FOUND", "Role não encontrada."));

        Usuario novo = new Usuario();
        novo.setEmail(dto.getEmail());
        novo.setNome(dto.getNome());
        novo.setSenha(passwordEncoder.encode(dto.getSenha()));
        novo.setRole(role);
        novo.setAtivo(true);
        // New users join the operator's empresa.
        novo.setEmpresaId(operador.getEmpresaId());

        Usuario saved = usuarioRepository.save(novo);
        audit.log(operador.getEmpresaId(), operador.getEmail(), operador.getId(), "USER_CREATED", "USUARIO", saved.getId(),
            "Novo usuário " + saved.getEmail() + " criado com role " + role.getNome(), null);
        return UsuarioResponseDTO.from(saved);
    }

    public UsuarioResponseDTO alterarRole(Long id, AlterarRoleRequestDTO dto, String emailOperador) {
        if (!ROLES_VALIDAS.contains(dto.getRoleNome())) {
            throw new ValidationException("INVALID_ROLE", "Role inválida: " + dto.getRoleNome());
        }

        Usuario operador = findByEmailOrThrow(emailOperador);
        Usuario alvo = findAlvoOrThrow(id, operador);

        if (alvo.getId().equals(operador.getId())) {
            throw new ForbiddenException("SELF_ROLE_CHANGE_FORBIDDEN",
                "Você não pode alterar sua própria role.");
        }

        validarPermissaoRole(operador, alvo.getRole().getNome()); // can manage the current target?
        validarPermissaoRole(operador, dto.getRoleNome());        // can assign the new role?

        Role novaRole = roleRepository.findByNome(dto.getRoleNome())
            .orElseThrow(() -> new NotFoundException("ROLE_NOT_FOUND", "Role não encontrada."));

        String roleAnterior = alvo.getRole().getNome();
        alvo.setRole(novaRole);
        Usuario saved = usuarioRepository.save(alvo);
        audit.log(operador.getEmpresaId(), operador.getEmail(), operador.getId(), "USER_ROLE_CHANGED", "USUARIO", saved.getId(),
            "Role de " + saved.getEmail() + ": " + roleAnterior + " -> " + novaRole.getNome(), null);
        return UsuarioResponseDTO.from(saved);
    }

    public UsuarioResponseDTO ativar(Long id, String emailOperador) {
        Usuario operador = findByEmailOrThrow(emailOperador);
        Usuario alvo = findAlvoOrThrow(id, operador);
        validarPermissaoRole(operador, alvo.getRole().getNome());
        alvo.setAtivo(true);
        Usuario saved = usuarioRepository.save(alvo);
        audit.log(operador.getEmpresaId(), operador.getEmail(), operador.getId(), "USER_ACTIVATED", "USUARIO", saved.getId(),
            "Usuário " + saved.getEmail() + " reativado", null);
        return UsuarioResponseDTO.from(saved);
    }

    public UsuarioResponseDTO desativar(Long id, String emailOperador) {
        Usuario operador = findByEmailOrThrow(emailOperador);
        Usuario alvo = findAlvoOrThrow(id, operador);
        if (alvo.getId().equals(operador.getId())) {
            throw new ForbiddenException("SELF_DEACTIVATE_FORBIDDEN",
                "Você não pode desativar sua própria conta.");
        }
        validarPermissaoRole(operador, alvo.getRole().getNome());
        alvo.setAtivo(false);
        Usuario saved = usuarioRepository.save(alvo);
        audit.log(operador.getEmpresaId(), operador.getEmail(), operador.getId(), "USER_DEACTIVATED", "USUARIO", saved.getId(),
            "Usuário " + saved.getEmail() + " desativado", null);
        return UsuarioResponseDTO.from(saved);
    }

    /**
     * Soft delete — stamps {@code deleted_at}. Original row stays for audit
     * + recovery; {@code @SQLRestriction} hides it from every read query.
     */
    public void deletar(Long id, String emailOperador) {
        Usuario operador = findByEmailOrThrow(emailOperador);
        Usuario alvo = findAlvoOrThrow(id, operador);
        if (alvo.getId().equals(operador.getId())) {
            throw new ForbiddenException("SELF_DELETE_FORBIDDEN",
                "Você não pode deletar sua própria conta.");
        }
        validarPermissaoRole(operador, alvo.getRole().getNome());
        String alvoEmail = alvo.getEmail();
        alvo.setDeletedAt(LocalDateTime.now());
        usuarioRepository.save(alvo);
        audit.log(operador.getEmpresaId(), operador.getEmail(), operador.getId(), "USER_DELETED", "USUARIO", id,
            "Usuário " + alvoEmail + " removido (soft delete)", null);
    }

    // ── helpers ──────────────────────────────────────────────────────────

    /** Loads the target user only if it lives in the operator's empresa (404 otherwise). */
    private Usuario findAlvoOrThrow(Long id, Usuario operador) {
        return usuarioRepository.findByIdAndEmpresaId(id, operador.getEmpresaId())
            .orElseThrow(() -> NotFoundException.of("Usuário", id));
    }

    private Usuario findByEmailOrThrow(String email) {
        return usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new NotFoundException("OPERATOR_NOT_FOUND", "Operador não encontrado."));
    }

    /**
     * SUPER_ADMIN can manage any role.
     * ADMIN can manage GESTOR, TECNICO and VISUALIZADOR (cannot touch SUPER_ADMIN or other ADMINs).
     */
    private void validarPermissaoRole(Usuario operador, String roleAlvo) {
        String roleOp = operador.getRole().getNome();
        if ("ROLE_SUPER_ADMIN".equals(roleOp)) return;
        if ("ROLE_ADMIN".equals(roleOp)) {
            if ("ROLE_SUPER_ADMIN".equals(roleAlvo) || "ROLE_ADMIN".equals(roleAlvo)) {
                throw new ForbiddenException("ADMIN_CANNOT_MANAGE_ADMIN",
                    "ADMIN não pode gerenciar usuários com role SUPER_ADMIN ou ADMIN.");
            }
            return;
        }
        throw new ForbiddenException("INSUFFICIENT_PRIVILEGES",
            "Sem permissão para gerenciar usuários.");
    }
}

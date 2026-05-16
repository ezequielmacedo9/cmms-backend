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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class UsuarioService {

    private static final Set<String> ROLES_VALIDAS = Set.of(
        "ROLE_SUPER_ADMIN", "ROLE_ADMIN", "ROLE_GESTOR", "ROLE_TECNICO", "ROLE_VISUALIZADOR"
    );

    private final UsuarioRepository usuarioRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          RoleRepository roleRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UsuarioResponseDTO> listar() {
        return usuarioRepository.findAllByOrderByDataCriacaoDesc()
            .stream()
            .map(UsuarioResponseDTO::from)
            .toList();
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

        Role role = roleRepository.findByNome(dto.getRoleNome())
            .orElseThrow(() -> new NotFoundException("ROLE_NOT_FOUND", "Role não encontrada."));

        Usuario novo = new Usuario();
        novo.setEmail(dto.getEmail());
        novo.setNome(dto.getNome());
        novo.setSenha(passwordEncoder.encode(dto.getSenha()));
        novo.setRole(role);
        novo.setAtivo(true);

        return UsuarioResponseDTO.from(usuarioRepository.save(novo));
    }

    public UsuarioResponseDTO alterarRole(Long id, AlterarRoleRequestDTO dto, String emailOperador) {
        if (!ROLES_VALIDAS.contains(dto.getRoleNome())) {
            throw new ValidationException("INVALID_ROLE", "Role inválida: " + dto.getRoleNome());
        }

        Usuario operador = findByEmailOrThrow(emailOperador);
        Usuario alvo = findByIdOrThrow(id);

        if (alvo.getId().equals(operador.getId())) {
            throw new ForbiddenException("SELF_ROLE_CHANGE_FORBIDDEN",
                "Você não pode alterar sua própria role.");
        }

        validarPermissaoRole(operador, alvo.getRole().getNome()); // can manage the current target?
        validarPermissaoRole(operador, dto.getRoleNome());        // can assign the new role?

        Role novaRole = roleRepository.findByNome(dto.getRoleNome())
            .orElseThrow(() -> new NotFoundException("ROLE_NOT_FOUND", "Role não encontrada."));

        alvo.setRole(novaRole);
        return UsuarioResponseDTO.from(usuarioRepository.save(alvo));
    }

    public UsuarioResponseDTO ativar(Long id, String emailOperador) {
        validarOperadorPodeGerenciarAlvo(id, emailOperador);
        Usuario u = findByIdOrThrow(id);
        u.setAtivo(true);
        return UsuarioResponseDTO.from(usuarioRepository.save(u));
    }

    public UsuarioResponseDTO desativar(Long id, String emailOperador) {
        Usuario operador = findByEmailOrThrow(emailOperador);
        if (findByIdOrThrow(id).getId().equals(operador.getId())) {
            throw new ForbiddenException("SELF_DEACTIVATE_FORBIDDEN",
                "Você não pode desativar sua própria conta.");
        }
        validarOperadorPodeGerenciarAlvo(id, emailOperador);
        Usuario u = findByIdOrThrow(id);
        u.setAtivo(false);
        return UsuarioResponseDTO.from(usuarioRepository.save(u));
    }

    public void deletar(Long id, String emailOperador) {
        Usuario operador = findByEmailOrThrow(emailOperador);
        if (findByIdOrThrow(id).getId().equals(operador.getId())) {
            throw new ForbiddenException("SELF_DELETE_FORBIDDEN",
                "Você não pode deletar sua própria conta.");
        }
        usuarioRepository.deleteById(id);
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private Usuario findByIdOrThrow(Long id) {
        return usuarioRepository.findById(id)
            .orElseThrow(() -> NotFoundException.of("Usuário", id));
    }

    private Usuario findByEmailOrThrow(String email) {
        return usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new NotFoundException("OPERATOR_NOT_FOUND", "Operador não encontrado."));
    }

    private void validarOperadorPodeGerenciarAlvo(Long idAlvo, String emailOperador) {
        Usuario operador = findByEmailOrThrow(emailOperador);
        Usuario alvo = findByIdOrThrow(idAlvo);
        validarPermissaoRole(operador, alvo.getRole().getNome());
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

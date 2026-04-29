package br.com.cmms.cmms.service;

import br.com.cmms.cmms.Security.TenantContext;
import br.com.cmms.cmms.dto.AlterarRoleRequestDTO;
import br.com.cmms.cmms.dto.ConvidarUsuarioRequestDTO;
import br.com.cmms.cmms.dto.UsuarioResponseDTO;
import br.com.cmms.cmms.model.Role;
import br.com.cmms.cmms.model.Usuario;
import br.com.cmms.cmms.repository.EmpresaRepository;
import br.com.cmms.cmms.repository.RoleRepository;
import br.com.cmms.cmms.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@Service
public class UsuarioService {

    private static final Set<String> ROLES_VALIDAS = Set.of(
        "ROLE_SUPER_ADMIN", "ROLE_ADMIN", "ROLE_GESTOR", "ROLE_TECNICO", "ROLE_VISUALIZADOR"
    );

    private final UsuarioRepository usuarioRepository;
    private final RoleRepository roleRepository;
    private final EmpresaRepository empresaRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          RoleRepository roleRepository,
                          EmpresaRepository empresaRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.roleRepository = roleRepository;
        this.empresaRepository = empresaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UsuarioResponseDTO> listar() {
        Long empresaId = TenantContext.getEmpresaId();
        if (empresaId == null) {
            return usuarioRepository.findAllByOrderByDataCriacaoDesc()
                .stream().map(UsuarioResponseDTO::from).toList();
        }
        return usuarioRepository.findAllByEmpresaIdOrderByDataCriacaoDesc(empresaId)
            .stream().map(UsuarioResponseDTO::from).toList();
    }

    public UsuarioResponseDTO getMeuPerfil(String email) {
        Usuario u = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
        return UsuarioResponseDTO.from(u);
    }

    public UsuarioResponseDTO convidar(ConvidarUsuarioRequestDTO dto, String emailOperador) {
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email já cadastrado");
        }
        if (!ROLES_VALIDAS.contains(dto.getRoleNome())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role inválida: " + dto.getRoleNome());
        }

        Usuario operador = findByEmailOrThrow(emailOperador);
        validarPermissaoRole(operador, dto.getRoleNome());

        Role role = roleRepository.findByNome(dto.getRoleNome())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role não encontrada"));

        Long empresaId = TenantContext.getEmpresaId();

        Usuario novo = new Usuario();
        novo.setEmail(dto.getEmail());
        novo.setNome(dto.getNome());
        novo.setSenha(passwordEncoder.encode(dto.getSenha()));
        novo.setRole(role);
        novo.setAtivo(true);
        if (empresaId != null) {
            novo.setEmpresa(empresaRepository.getReferenceById(empresaId));
        }

        return UsuarioResponseDTO.from(usuarioRepository.save(novo));
    }

    public UsuarioResponseDTO alterarRole(Long id, AlterarRoleRequestDTO dto, String emailOperador) {
        if (!ROLES_VALIDAS.contains(dto.getRoleNome())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role inválida: " + dto.getRoleNome());
        }

        Usuario operador = findByEmailOrThrow(emailOperador);
        Usuario alvo = findByIdOrThrow(id);

        requireSameEmpresa(operador, alvo);

        if (alvo.getId().equals(operador.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não pode alterar sua própria role");
        }

        validarPermissaoRole(operador, alvo.getRole().getNome());
        validarPermissaoRole(operador, dto.getRoleNome());

        Role novaRole = roleRepository.findByNome(dto.getRoleNome())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role não encontrada"));

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
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não pode desativar sua própria conta");
        }
        validarOperadorPodeGerenciarAlvo(id, emailOperador);
        Usuario u = findByIdOrThrow(id);
        u.setAtivo(false);
        return UsuarioResponseDTO.from(usuarioRepository.save(u));
    }

    public void deletar(Long id, String emailOperador) {
        Usuario operador = findByEmailOrThrow(emailOperador);
        if (findByIdOrThrow(id).getId().equals(operador.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não pode deletar sua própria conta");
        }
        usuarioRepository.deleteById(id);
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private Usuario findByIdOrThrow(Long id) {
        return usuarioRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
    }

    private Usuario findByEmailOrThrow(String email) {
        return usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Operador não encontrado"));
    }

    private void validarOperadorPodeGerenciarAlvo(Long idAlvo, String emailOperador) {
        Usuario operador = findByEmailOrThrow(emailOperador);
        Usuario alvo = findByIdOrThrow(idAlvo);
        requireSameEmpresa(operador, alvo);
        validarPermissaoRole(operador, alvo.getRole().getNome());
    }

    private void requireSameEmpresa(Usuario operador, Usuario alvo) {
        if (operador.getEmpresa() == null || alvo.getEmpresa() == null) return;
        if (!operador.getEmpresa().getId().equals(alvo.getEmpresa().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário pertence a outra empresa");
        }
    }

    private void validarPermissaoRole(Usuario operador, String roleAlvo) {
        String roleOp = operador.getRole().getNome();
        if ("ROLE_SUPER_ADMIN".equals(roleOp)) return;
        if ("ROLE_ADMIN".equals(roleOp)) {
            if ("ROLE_SUPER_ADMIN".equals(roleAlvo) || "ROLE_ADMIN".equals(roleAlvo)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "ADMIN não pode gerenciar usuários com role SUPER_ADMIN ou ADMIN");
            }
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sem permissão para gerenciar usuários");
    }
}

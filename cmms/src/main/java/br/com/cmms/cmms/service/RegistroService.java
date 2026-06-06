package br.com.cmms.cmms.service;

import br.com.cmms.cmms.dto.RegistroRequestDTO;
import br.com.cmms.cmms.dto.TokenResponseDTO;
import br.com.cmms.cmms.exception.ConflictException;
import br.com.cmms.cmms.model.Empresa;
import br.com.cmms.cmms.model.Role;
import br.com.cmms.cmms.model.Usuario;
import br.com.cmms.cmms.repository.EmpresaRepository;
import br.com.cmms.cmms.repository.RoleRepository;
import br.com.cmms.cmms.repository.UsuarioRepository;
import br.com.cmms.cmms.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Self-service tenant onboarding: atomically creates an {@link Empresa} and
 * its first administrator, then issues tokens so the caller is logged straight
 * in. The new admin is bound to the freshly created empresa.
 */
@Service
public class RegistroService {

    private static final Logger log = LoggerFactory.getLogger(RegistroService.class);
    private static final String ADMIN_ROLE = "ROLE_ADMIN";

    private final EmpresaRepository empresaRepo;
    private final UsuarioRepository usuarioRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuditService audit;

    public RegistroService(EmpresaRepository empresaRepo,
                           UsuarioRepository usuarioRepo,
                           RoleRepository roleRepo,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           RefreshTokenService refreshTokenService,
                           AuditService audit) {
        this.empresaRepo = empresaRepo;
        this.usuarioRepo = usuarioRepo;
        this.roleRepo = roleRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.audit = audit;
    }

    @Transactional
    public TokenResponseDTO registrar(RegistroRequestDTO dto, HttpServletRequest request) {
        if (usuarioRepo.existsByEmail(dto.getEmail())) {
            throw new ConflictException("EMAIL_ALREADY_REGISTERED", "Email já cadastrado.");
        }

        Role adminRole = roleRepo.findByNome(ADMIN_ROLE)
            .orElseThrow(() -> new IllegalStateException("Required role missing: " + ADMIN_ROLE));

        Empresa empresa = empresaRepo.save(new Empresa(dto.getEmpresaNome().trim()));

        Usuario admin = new Usuario();
        admin.setEmail(dto.getEmail());
        admin.setNome(dto.getNome());
        admin.setSenha(passwordEncoder.encode(dto.getSenha()));
        admin.setRole(adminRole);
        admin.setAtivo(true);
        admin.setEmpresaId(empresa.getId());
        Usuario saved = usuarioRepo.save(admin);

        log.info("Nova empresa registrada: '{}' (admin {})", empresa.getNome(), saved.getEmail());
        audit.log(empresa.getId(), saved.getEmail(), saved.getId(), "EMPRESA_REGISTERED", "EMPRESA",
            empresa.getId(), "Nova empresa '" + empresa.getNome() + "' criada", AuditService.getClientIp(request));

        String accessToken  = jwtService.gerarToken(saved);
        String refreshToken = refreshTokenService.criarRefreshToken(saved).getToken();
        return new TokenResponseDTO(
            accessToken, refreshToken,
            saved.getRole().getNome(),
            saved.getNome() != null ? saved.getNome() : saved.getEmail(),
            saved.getId()
        );
    }
}

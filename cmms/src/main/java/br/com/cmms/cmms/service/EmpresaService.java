package br.com.cmms.cmms.service;

import br.com.cmms.cmms.Security.JwtService;
import br.com.cmms.cmms.Security.TenantContext;
import br.com.cmms.cmms.dto.EmpresaResponseDTO;
import br.com.cmms.cmms.dto.RegisterRequestDTO;
import br.com.cmms.cmms.dto.TokenResponseDTO;
import br.com.cmms.cmms.dto.UsuarioResponseDTO;
import br.com.cmms.cmms.model.Empresa;
import br.com.cmms.cmms.model.PlanoAssinatura;
import br.com.cmms.cmms.model.Role;
import br.com.cmms.cmms.model.Usuario;
import br.com.cmms.cmms.repository.EmpresaRepository;
import br.com.cmms.cmms.repository.RoleRepository;
import br.com.cmms.cmms.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
public class EmpresaService {

    private static final Logger log = LoggerFactory.getLogger(EmpresaService.class);

    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;

    public EmpresaService(EmpresaRepository empresaRepository,
                          UsuarioRepository usuarioRepository,
                          RoleRepository roleRepository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService,
                          RefreshTokenService refreshTokenService,
                          EmailService emailService) {
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.emailService = emailService;
    }

    @Transactional
    public Map<String, Object> register(RegisterRequestDTO dto) {
        String cnpj = normalizeCnpj(dto.getCnpj());

        if (!isValidCnpjFormat(cnpj)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CNPJ inválido");
        }
        if (empresaRepository.existsByCnpj(cnpj)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CNPJ já cadastrado");
        }
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email já cadastrado");
        }

        Empresa empresa = new Empresa();
        empresa.setNome(dto.getNomeEmpresa());
        empresa.setCnpj(cnpj);
        empresa.setEmail(dto.getEmail());
        empresa.setTelefone(dto.getTelefone());
        empresa.setPlano(PlanoAssinatura.STARTER);
        empresa = empresaRepository.save(empresa);

        Role adminRole = roleRepository.findByNome("ROLE_ADMIN")
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Role ADMIN não encontrada"));

        Usuario admin = new Usuario();
        admin.setNome(dto.getNome());
        admin.setEmail(dto.getEmail());
        admin.setSenha(passwordEncoder.encode(dto.getSenha()));
        admin.setTelefone(dto.getTelefone());
        admin.setRole(adminRole);
        admin.setEmpresa(empresa);
        admin = usuarioRepository.save(admin);

        log.info("Empresa registrada: {} (CNPJ={}) admin={}", empresa.getNome(), cnpj, admin.getEmail());

        try {
            emailService.sendBoasVindas(admin.getEmail(), admin.getNome(), empresa.getNome());
        } catch (Exception e) {
            log.warn("Falha ao enviar email de boas-vindas para {}: {}", admin.getEmail(), e.getMessage());
        }

        String accessToken = jwtService.gerarToken(admin);
        String refreshToken = refreshTokenService.criarRefreshToken(admin).getToken();

        return Map.of(
            "accessToken", accessToken,
            "refreshToken", refreshToken,
            "role", admin.getRole().getNome(),
            "nome", admin.getNome() != null ? admin.getNome() : admin.getEmail(),
            "usuarioId", admin.getId(),
            "empresa", EmpresaResponseDTO.from(empresa)
        );
    }

    public EmpresaResponseDTO getMinha() {
        return EmpresaResponseDTO.from(getEmpresaAtual());
    }

    @Transactional
    public EmpresaResponseDTO updateMinha(EmpresaResponseDTO dto) {
        Empresa empresa = getEmpresaAtual();
        if (dto.nome() != null && !dto.nome().isBlank()) empresa.setNome(dto.nome());
        if (dto.email() != null) empresa.setEmail(dto.email());
        if (dto.telefone() != null) empresa.setTelefone(dto.telefone());
        if (dto.endereco() != null) empresa.setEndereco(dto.endereco());
        return EmpresaResponseDTO.from(empresaRepository.save(empresa));
    }

    public List<UsuarioResponseDTO> listarUsuarios() {
        Long empresaId = requireEmpresaId();
        return usuarioRepository.findAllByEmpresaIdOrderByDataCriacaoDesc(empresaId)
            .stream().map(UsuarioResponseDTO::from).toList();
    }

    @Transactional
    public void removerUsuario(Long usuarioId, String emailOperador) {
        Long empresaId = requireEmpresaId();
        Usuario alvo = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        if (alvo.getEmpresa() == null || !empresaId.equals(alvo.getEmpresa().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário não pertence à sua empresa");
        }

        Usuario operador = usuarioRepository.findByEmail(emailOperador)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Operador não encontrado"));

        if (alvo.getId().equals(operador.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não pode remover sua própria conta");
        }

        usuarioRepository.deleteById(usuarioId);
        log.info("Usuário {} removido da empresa {} por {}", usuarioId, empresaId, emailOperador);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Empresa getEmpresaAtual() {
        Long empresaId = requireEmpresaId();
        return empresaRepository.findById(empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa não encontrada"));
    }

    private Long requireEmpresaId() {
        Long id = TenantContext.getEmpresaId();
        if (id == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Empresa não associada ao usuário");
        return id;
    }

    private String normalizeCnpj(String cnpj) {
        return cnpj == null ? "" : cnpj.replaceAll("[^0-9]", "");
    }

    private boolean isValidCnpjFormat(String digits) {
        return digits != null && digits.length() == 14;
    }
}

package br.com.cmms.cmms.config;

import br.com.cmms.cmms.model.Empresa;
import br.com.cmms.cmms.model.PlanoAssinatura;
import br.com.cmms.cmms.model.Role;
import br.com.cmms.cmms.model.Usuario;
import br.com.cmms.cmms.repository.EmpresaRepository;
import br.com.cmms.cmms.repository.RoleRepository;
import br.com.cmms.cmms.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UsuarioRepository usuarioRepository;
    private final RoleRepository roleRepository;
    private final EmpresaRepository empresaRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UsuarioRepository usuarioRepository,
                           RoleRepository roleRepository,
                           EmpresaRepository empresaRepository,
                           PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.roleRepository = roleRepository;
        this.empresaRepository = empresaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        Role superAdminRole = ensureRole("ROLE_SUPER_ADMIN");
        Role adminRole      = ensureRole("ROLE_ADMIN");
        ensureRole("ROLE_GESTOR");
        ensureRole("ROLE_TECNICO");
        ensureRole("ROLE_VISUALIZADOR");
        ensureRole("ROLE_USER");

        Empresa empresa = ensureEmpresa("Demo Industrial", "00.000.000/0001-00");

        ensureUsuario("superadmin@email.com", "Super Administrador", "123456", superAdminRole, empresa);
        ensureUsuario("admin@email.com",      "Administrador",       "123456", adminRole,      empresa);

        // Assign default empresa to any existing users without one (migration)
        usuarioRepository.findAll().forEach(u -> {
            if (u.getEmpresa() == null) {
                u.setEmpresa(empresa);
                usuarioRepository.save(u);
            }
        });
    }

    private Role ensureRole(String nome) {
        return roleRepository.findByNome(nome).orElseGet(() -> {
            Role r = new Role(nome);
            return roleRepository.save(r);
        });
    }

    private Empresa ensureEmpresa(String nome, String cnpj) {
        return empresaRepository.findByCnpj(cnpj).orElseGet(() -> {
            Empresa e = new Empresa();
            e.setNome(nome);
            e.setCnpj(cnpj);
            e.setPlano(PlanoAssinatura.ENTERPRISE);
            e.setAtivo(true);
            Empresa saved = empresaRepository.save(e);
            log.info("Empresa default criada: {} [{}]", nome, saved.getId());
            return saved;
        });
    }

    private void ensureUsuario(String email, String nome, String senhaPlana, Role role, Empresa empresa) {
        Usuario u = usuarioRepository.findByEmail(email).orElse(null);
        if (u == null) {
            u = new Usuario();
            u.setEmail(email);
            u.setSenha(passwordEncoder.encode(senhaPlana));
        }
        if (u.getNome() == null) u.setNome(nome);
        if (!u.isAtivo()) u.setAtivo(true);
        u.setRole(role);
        u.setEmpresa(empresa);
        usuarioRepository.save(u);
        log.info("Usuario garantido: {} [{}] empresa={}", email, role.getNome(), empresa.getNome());
    }
}

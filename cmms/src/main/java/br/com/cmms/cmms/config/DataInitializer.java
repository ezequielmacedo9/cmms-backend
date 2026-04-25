package br.com.cmms.cmms.config;

import br.com.cmms.cmms.model.Role;
import br.com.cmms.cmms.model.Usuario;
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
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UsuarioRepository usuarioRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        Role superAdminRole = ensureRole("ROLE_SUPER_ADMIN");
        Role adminRole      = ensureRole("ROLE_ADMIN");
        ensureRole("ROLE_GESTOR");
        ensureRole("ROLE_TECNICO");
        ensureRole("ROLE_VISUALIZADOR");
        ensureRole("ROLE_USER"); // backward compat

        ensureUsuario("superadmin@email.com", "Super Administrador", "123456", superAdminRole);
        ensureUsuario("admin@email.com",      "Administrador",       "123456", adminRole);
    }

    private Role ensureRole(String nome) {
        return roleRepository.findByNome(nome).orElseGet(() -> {
            Role r = new Role(nome);
            return roleRepository.save(r);
        });
    }

    private void ensureUsuario(String email, String nome, String senhaPlana, Role role) {
        Usuario u = usuarioRepository.findByEmail(email).orElse(null);
        if (u == null) {
            u = new Usuario();
            u.setEmail(email);
            u.setSenha(passwordEncoder.encode(senhaPlana));
        }
        if (u.getNome() == null) u.setNome(nome);
        if (!u.isAtivo()) u.setAtivo(true);
        u.setRole(role);
        usuarioRepository.save(u);
        log.info("Usuario garantido: {} [{}]", email, role.getNome());
    }
}

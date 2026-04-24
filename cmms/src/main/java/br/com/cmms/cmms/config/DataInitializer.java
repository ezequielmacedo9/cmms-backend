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
        Role role = roleRepository.findByNome("ROLE_ADMIN")
                .orElseGet(() -> {
                    Role r = new Role();
                    r.setNome("ROLE_ADMIN");
                    return roleRepository.save(r);
                });

        boolean exists = usuarioRepository.findByEmail("admin@email.com").isPresent();
        if (!exists) {
            Usuario admin = new Usuario();
            admin.setEmail("admin@email.com");
            admin.setSenha(passwordEncoder.encode("123456"));
            admin.setRole(role);
            usuarioRepository.save(admin);
            log.info("Admin user created: admin@email.com");
        } else {
            log.info("Admin user already exists, skipping seed.");
        }
    }
}

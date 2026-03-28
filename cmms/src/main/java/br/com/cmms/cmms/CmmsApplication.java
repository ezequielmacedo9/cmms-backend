package br.com.cmms.cmms;

import br.com.cmms.cmms.model.Role;
import br.com.cmms.cmms.model.Usuario;
import br.com.cmms.cmms.repository.RoleRepository;
import br.com.cmms.cmms.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;

@SpringBootApplication
public class CmmsApplication {

    private static final Logger log = LoggerFactory.getLogger(CmmsApplication.class);

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(CmmsApplication.class);
        String port = System.getenv("PORT");
        if (port != null) {
            app.setDefaultProperties(Collections.singletonMap("server.port", port));
        }
        app.run(args);
    }

    @Bean
    CommandLineRunner initAdmin(
            UsuarioRepository usuarioRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            Role adminRole = roleRepository.findByNome("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN")));

            roleRepository.findByNome("ROLE_USER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_USER")));

            String emailAdmin = "admin@email.com";
            if (usuarioRepository.findByEmail(emailAdmin).isEmpty()) {
                Usuario admin = new Usuario();
                admin.setEmail(emailAdmin);
                admin.setSenha(passwordEncoder.encode("123456"));
                admin.setRole(adminRole);
                usuarioRepository.save(admin);
                log.info("Admin user created: {}", emailAdmin);
            } else {
                log.info("Admin user already exists");
            }
        };
    }
}

package br.com.cmms.cmms;

import br.com.cmms.cmms.model.Usuario;
import br.com.cmms.cmms.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import br.com.cmms.cmms.model.Role;
import br.com.cmms.cmms.repository.RoleRepository;

import java.util.Collections;

@SpringBootApplication
public class CmmsApplication {

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
                    .orElseGet(() -> {
                        Role newRole = new Role("ROLE_ADMIN");
                        return roleRepository.save(newRole);
                    });

            Role userRole = roleRepository.findByNome("ROLE_USER")
                    .orElseGet(() -> {
                        Role newRole = new Role("ROLE_USER");
                        return roleRepository.save(newRole);
                    });

            // 2. Criar admin se não existir
            String emailAdmin = "admin@email.com";

            if (usuarioRepository.findByEmail(emailAdmin).isEmpty()) {
                Usuario admin = new Usuario();
                admin.setEmail(emailAdmin);
                admin.setSenha(passwordEncoder.encode("123456"));
                admin.setRole(adminRole);

                usuarioRepository.save(admin);
                System.out.println(">>> ADMIN CRIADO! Email: " + emailAdmin + " | Senha: 123456 | Role: " + adminRole.getNome());
            } else {
                System.out.println(">>> Admin já existe.");
            }

            String emailUser = "user@email.com";
            if (usuarioRepository.findByEmail(emailUser).isEmpty()) {
                Usuario user = new Usuario();
                user.setEmail(emailUser);
                user.setSenha(passwordEncoder.encode("123456"));
                user.setRole(userRole);
                usuarioRepository.save(user);
                System.out.println(">>> USER COMUM CRIADO! Email: " + emailUser);
            }
        };
    }
}


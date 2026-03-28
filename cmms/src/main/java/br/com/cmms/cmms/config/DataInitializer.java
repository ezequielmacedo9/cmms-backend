package br.com.cmms.cmms.config;
import br.com.cmms.cmms.model.Role;
import br.com.cmms.cmms.model.Usuario;
import br.com.cmms.cmms.repository.RoleRepository;
import br.com.cmms.cmms.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
@Component
public class DataInitializer implements CommandLineRunner {
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
        Usuario admin = usuarioRepository.findByEmail("admin@email.com")
                .orElseGet(() -> {
                    Usuario u = new Usuario();
                    u.setEmail("admin@email.com");
                    u.setRole(role);
                    return u;
                });
        admin.setSenha(passwordEncoder.encode("123456"));
        usuarioRepository.save(admin);
        System.out.println(">>> Admin senha atualizada: 123456");
    }
}
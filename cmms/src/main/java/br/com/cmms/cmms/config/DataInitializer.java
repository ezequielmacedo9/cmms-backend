package br.com.cmms.cmms.config;

import br.com.cmms.cmms.model.Role;
import br.com.cmms.cmms.model.Usuario;
import br.com.cmms.cmms.repository.RoleRepository;
import br.com.cmms.cmms.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Application bootstrap routine.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Always ensures the base role catalog exists. Without it, any role
 *       assignment would fail at runtime.</li>
 *   <li>In <strong>dev</strong>: creates two convenience users
 *       ({@code superadmin@email.com} / {@code admin@email.com}) with the
 *       password defined in {@code app.bootstrap.dev-password} (default
 *       {@code dev123!}). These users only exist for local development.</li>
 *   <li>In <strong>prod</strong>: creates a SUPER_ADMIN account only when
 *       both {@code SUPER_ADMIN_EMAIL} and {@code SUPER_ADMIN_PASSWORD}
 *       environment variables are provided. The password must be at least
 *       {@link #MIN_PROD_PASSWORD_LENGTH} characters long. If the variables
 *       are missing, the application starts normally and no bootstrap user
 *       is created — the operator is expected to provision the first user
 *       through SQL or a future admin endpoint.</li>
 * </ul>
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    /** Canonical role catalog. Order is irrelevant. */
    private static final List<String> BASE_ROLES = List.of(
        "ROLE_SUPER_ADMIN",
        "ROLE_ADMIN",
        "ROLE_GESTOR",
        "ROLE_TECNICO",
        "ROLE_VISUALIZADOR",
        "ROLE_USER" // backward compatibility
    );

    private static final int MIN_PROD_PASSWORD_LENGTH = 12;
    private static final String PROD_PROFILE = "prod";

    private final UsuarioRepository usuarioRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment env;

    @Value("${app.bootstrap.dev-password:dev123!}")
    private String devPassword;

    @Value("${app.bootstrap.super-admin.email:}")
    private String superAdminEmail;

    @Value("${app.bootstrap.super-admin.password:}")
    private String superAdminPassword;

    public DataInitializer(UsuarioRepository usuarioRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder,
                           Environment env) {
        this.usuarioRepository = usuarioRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.env = env;
    }

    @Override
    public void run(String... args) {
        BASE_ROLES.forEach(this::ensureRole);

        if (isProd()) {
            bootstrapProd();
        } else {
            bootstrapDev();
        }
    }

    private boolean isProd() {
        return Arrays.asList(env.getActiveProfiles()).contains(PROD_PROFILE);
    }

    /**
     * Dev bootstrap: creates two convenience accounts so a fresh local clone
     * can be used immediately. Passwords are configurable via
     * {@code app.bootstrap.dev-password} and default to {@code dev123!}.
     */
    private void bootstrapDev() {
        Role superAdminRole = requireRole("ROLE_SUPER_ADMIN");
        Role adminRole      = requireRole("ROLE_ADMIN");

        ensureUsuario("superadmin@email.com", "Super Administrador", devPassword, superAdminRole);
        ensureUsuario("admin@email.com",      "Administrador",       devPassword, adminRole);
        log.warn("DEV bootstrap users created. Do NOT use these credentials outside local development.");
    }

    /**
     * Prod bootstrap: creates a SUPER_ADMIN only when explicit env vars are
     * present and the user does not yet exist. Fails fast if the supplied
     * password is too weak; this protects against accidentally provisioning
     * a trivial password in production.
     */
    private void bootstrapProd() {
        if (isBlank(superAdminEmail) || isBlank(superAdminPassword)) {
            log.info("Prod bootstrap skipped: SUPER_ADMIN_EMAIL / SUPER_ADMIN_PASSWORD not set.");
            return;
        }
        if (superAdminPassword.length() < MIN_PROD_PASSWORD_LENGTH) {
            throw new IllegalStateException(
                "SUPER_ADMIN_PASSWORD must be at least " + MIN_PROD_PASSWORD_LENGTH +
                " characters long. Refusing to bootstrap an admin with a weak password."
            );
        }
        if (usuarioRepository.findByEmail(superAdminEmail).isPresent()) {
            log.info("Prod bootstrap skipped: user {} already exists.", superAdminEmail);
            return;
        }
        Role superAdminRole = requireRole("ROLE_SUPER_ADMIN");
        ensureUsuario(superAdminEmail, "Super Administrador", superAdminPassword, superAdminRole);
        log.info("Prod bootstrap: SUPER_ADMIN account created for {}.", superAdminEmail);
    }

    private Role ensureRole(String nome) {
        return roleRepository.findByNome(nome).orElseGet(() -> roleRepository.save(new Role(nome)));
    }

    private Role requireRole(String nome) {
        return roleRepository.findByNome(nome)
            .orElseThrow(() -> new IllegalStateException("Required role missing: " + nome));
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
        log.info("User ensured: {} [{}]", email, role.getNome());
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}

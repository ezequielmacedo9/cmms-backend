package br.com.cmms.cmms.service;

import br.com.cmms.cmms.dto.AlterarRoleRequestDTO;
import br.com.cmms.cmms.dto.ConvidarUsuarioRequestDTO;
import br.com.cmms.cmms.exception.ConflictException;
import br.com.cmms.cmms.exception.ForbiddenException;
import br.com.cmms.cmms.exception.NotFoundException;
import br.com.cmms.cmms.exception.ValidationException;
import br.com.cmms.cmms.model.Role;
import br.com.cmms.cmms.model.Usuario;
import br.com.cmms.cmms.repository.RoleRepository;
import br.com.cmms.cmms.repository.UsuarioRepository;
import br.com.cmms.cmms.security.TenantResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UsuarioService} — focuses on the role-hierarchy
 * rules, which are the most subtle business logic in the codebase.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock UsuarioRepository usuarioRepository;
    @Mock RoleRepository roleRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuditService audit;
    @Mock TenantResolver tenant;
    @Mock AssinaturaService assinatura;

    @InjectMocks UsuarioService usuarioService;

    private Usuario superAdmin;
    private Usuario admin;
    private Usuario gestor;
    private Usuario tecnico;

    @BeforeEach
    void setUp() {
        superAdmin = user(1L, "super@cmms.app", "ROLE_SUPER_ADMIN");
        admin      = user(2L, "admin@cmms.app", "ROLE_ADMIN");
        gestor     = user(3L, "gestor@cmms.app", "ROLE_GESTOR");
        tecnico    = user(4L, "tec@cmms.app", "ROLE_TECNICO");

        lenient().when(passwordEncoder.encode(any())).thenReturn("HASHED");
    }

    @Test
    @DisplayName("convidar: rejeita email duplicado (ConflictException)")
    void convidar_duplicateEmail() {
        ConvidarUsuarioRequestDTO dto = dto("nome", "x@cmms.app", "secret123", "ROLE_TECNICO");
        when(usuarioRepository.existsByEmail("x@cmms.app")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.convidar(dto, superAdmin.getEmail()))
            .isInstanceOf(ConflictException.class)
            .extracting("errorCode").isEqualTo("EMAIL_ALREADY_REGISTERED");
    }

    @Test
    @DisplayName("convidar: rejeita role fora do catálogo")
    void convidar_invalidRole() {
        ConvidarUsuarioRequestDTO dto = dto("nome", "novo@cmms.app", "secret123", "ROLE_GOD");
        when(usuarioRepository.existsByEmail("novo@cmms.app")).thenReturn(false);

        assertThatThrownBy(() -> usuarioService.convidar(dto, superAdmin.getEmail()))
            .isInstanceOf(ValidationException.class)
            .extracting("errorCode").isEqualTo("INVALID_ROLE");
    }

    @Test
    @DisplayName("convidar: SUPER_ADMIN pode criar qualquer role")
    void convidar_superAdminCanCreateAnything() {
        ConvidarUsuarioRequestDTO dto = dto("Novo SA", "newsa@cmms.app", "secret123", "ROLE_SUPER_ADMIN");
        when(usuarioRepository.existsByEmail("newsa@cmms.app")).thenReturn(false);
        when(usuarioRepository.findByEmail(superAdmin.getEmail())).thenReturn(Optional.of(superAdmin));
        Role newRole = new Role("ROLE_SUPER_ADMIN");
        when(roleRepository.findByNome("ROLE_SUPER_ADMIN")).thenReturn(Optional.of(newRole));
        when(usuarioRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var saved = usuarioService.convidar(dto, superAdmin.getEmail());

        assertThat(saved.getRole()).isEqualTo("ROLE_SUPER_ADMIN");
    }

    @Test
    @DisplayName("convidar: ADMIN não pode criar outro ADMIN ou SUPER_ADMIN")
    void convidar_adminCannotEscalate() {
        ConvidarUsuarioRequestDTO dto = dto("Novo Admin", "na@cmms.app", "secret123", "ROLE_ADMIN");
        when(usuarioRepository.existsByEmail("na@cmms.app")).thenReturn(false);
        when(usuarioRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> usuarioService.convidar(dto, admin.getEmail()))
            .isInstanceOf(ForbiddenException.class)
            .extracting("errorCode").isEqualTo("ADMIN_CANNOT_MANAGE_ADMIN");
    }

    @Test
    @DisplayName("convidar: ADMIN pode criar GESTOR/TECNICO/VISUALIZADOR")
    void convidar_adminCanCreateLowerRoles() {
        ConvidarUsuarioRequestDTO dto = dto("Novo Tec", "nt@cmms.app", "secret123", "ROLE_TECNICO");
        when(usuarioRepository.existsByEmail("nt@cmms.app")).thenReturn(false);
        when(usuarioRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        Role tec = new Role("ROLE_TECNICO");
        when(roleRepository.findByNome("ROLE_TECNICO")).thenReturn(Optional.of(tec));
        when(usuarioRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var saved = usuarioService.convidar(dto, admin.getEmail());

        assertThat(saved.getRole()).isEqualTo("ROLE_TECNICO");
    }

    @Test
    @DisplayName("alterarRole: ninguém pode alterar a própria role")
    void alterarRole_selfNotAllowed() {
        AlterarRoleRequestDTO dto = new AlterarRoleRequestDTO();
        dto.setRoleNome("ROLE_ADMIN");
        when(usuarioRepository.findByEmail(superAdmin.getEmail())).thenReturn(Optional.of(superAdmin));
        when(usuarioRepository.findByIdAndEmpresaId(superAdmin.getId(), 1L)).thenReturn(Optional.of(superAdmin));

        assertThatThrownBy(() -> usuarioService.alterarRole(superAdmin.getId(), dto, superAdmin.getEmail()))
            .isInstanceOf(ForbiddenException.class)
            .extracting("errorCode").isEqualTo("SELF_ROLE_CHANGE_FORBIDDEN");
    }

    @Test
    @DisplayName("alterarRole: ADMIN não pode promover GESTOR para ADMIN")
    void alterarRole_adminCannotPromoteToAdmin() {
        AlterarRoleRequestDTO dto = new AlterarRoleRequestDTO();
        dto.setRoleNome("ROLE_ADMIN");
        when(usuarioRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(usuarioRepository.findByIdAndEmpresaId(gestor.getId(), 1L)).thenReturn(Optional.of(gestor));

        assertThatThrownBy(() -> usuarioService.alterarRole(gestor.getId(), dto, admin.getEmail()))
            .isInstanceOf(ForbiddenException.class)
            .extracting("errorCode").isEqualTo("ADMIN_CANNOT_MANAGE_ADMIN");
    }

    @Test
    @DisplayName("alterarRole: ADMIN pode promover TECNICO para GESTOR")
    void alterarRole_adminCanPromoteWithinAllowedRoles() {
        AlterarRoleRequestDTO dto = new AlterarRoleRequestDTO();
        dto.setRoleNome("ROLE_GESTOR");
        when(usuarioRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(usuarioRepository.findByIdAndEmpresaId(tecnico.getId(), 1L)).thenReturn(Optional.of(tecnico));
        Role gRole = new Role("ROLE_GESTOR");
        when(roleRepository.findByNome("ROLE_GESTOR")).thenReturn(Optional.of(gRole));
        when(usuarioRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var saved = usuarioService.alterarRole(tecnico.getId(), dto, admin.getEmail());

        assertThat(saved.getRole()).isEqualTo("ROLE_GESTOR");
    }

    @Test
    @DisplayName("desativar: ninguém pode desativar a si próprio")
    void desativar_selfNotAllowed() {
        when(usuarioRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(usuarioRepository.findByIdAndEmpresaId(admin.getId(), 1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> usuarioService.desativar(admin.getId(), admin.getEmail()))
            .isInstanceOf(ForbiddenException.class)
            .extracting("errorCode").isEqualTo("SELF_DEACTIVATE_FORBIDDEN");
    }

    @Test
    @DisplayName("getMeuPerfil: 404 quando o usuário não existe")
    void getMeuPerfil_notFound() {
        when(usuarioRepository.findByEmail("ghost@cmms.app")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.getMeuPerfil("ghost@cmms.app"))
            .isInstanceOf(NotFoundException.class);
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private static Usuario user(Long id, String email, String roleName) {
        Usuario u = new Usuario();
        ReflectionTestUtils.setField(u, "id", id);
        u.setEmail(email);
        u.setAtivo(true);
        u.setEmpresaId(1L);
        u.setRole(new Role(roleName));
        return u;
    }

    private static ConvidarUsuarioRequestDTO dto(String nome, String email, String senha, String role) {
        ConvidarUsuarioRequestDTO d = new ConvidarUsuarioRequestDTO();
        d.setNome(nome); d.setEmail(email); d.setSenha(senha); d.setRoleNome(role);
        return d;
    }
}

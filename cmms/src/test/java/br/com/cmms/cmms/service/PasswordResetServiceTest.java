package br.com.cmms.cmms.service;

import br.com.cmms.cmms.exception.UnauthorizedException;
import br.com.cmms.cmms.exception.ValidationException;
import br.com.cmms.cmms.model.PasswordResetToken;
import br.com.cmms.cmms.model.Usuario;
import br.com.cmms.cmms.repository.PasswordResetTokenRepository;
import br.com.cmms.cmms.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit tests for {@link PasswordResetService} (request, validate, consume). */
@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock UsuarioRepository usuarioRepo;
    @Mock PasswordResetTokenRepository tokenRepo;
    @Mock EmailService emailService;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuditService audit;
    @Mock HttpServletRequest request;
    @InjectMocks PasswordResetService service;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", 3L);
        usuario.setEmail("ana@cmms.app");
        usuario.setNome("Ana");
        lenient().when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        lenient().when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    }

    @Test
    @DisplayName("requestReset: email desconhecido e silencioso (anti-enumeracao)")
    void requestReset_emailDesconhecido_silencioso() {
        when(usuarioRepo.findByEmail("ghost@cmms.app")).thenReturn(Optional.empty());

        assertThatCode(() -> service.requestReset("ghost@cmms.app", request)).doesNotThrowAnyException();

        verify(tokenRepo, never()).save(any());
        verify(emailService, never()).sendPasswordReset(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("requestReset: gera token de 64 hex, envia email e audita")
    void requestReset_happyPath() {
        when(usuarioRepo.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));

        service.requestReset(usuario.getEmail(), request);

        ArgumentCaptor<PasswordResetToken> saved = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepo).save(saved.capture());
        assertThat(saved.getValue().getToken()).matches("[0-9a-f]{64}");
        assertThat(saved.getValue().getExpiresAt()).isAfter(LocalDateTime.now());
        verify(emailService).sendPasswordReset(eq(usuario.getEmail()), anyString(), eq("Ana"));
        verify(audit).log(any(), eq(usuario.getEmail()), any(), eq("PASSWORD_RESET_REQUEST"),
            eq("USUARIO"), any(), anyString(), anyString());
    }

    @Test
    @DisplayName("requestReset: falha no envio de email nao propaga (token ja salvo)")
    void requestReset_emailFalha_naoPropaga() {
        when(usuarioRepo.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
        doThrow(new RuntimeException("smtp down"))
            .when(emailService).sendPasswordReset(anyString(), anyString(), anyString());

        assertThatCode(() -> service.requestReset(usuario.getEmail(), request)).doesNotThrowAnyException();
        verify(tokenRepo).save(any());
    }

    @Test
    @DisplayName("isTokenValid: valido=true; usado/expirado/desconhecido=false")
    void isTokenValid_estados() {
        PasswordResetToken ok = tokenFor(usuario, 1, false);
        PasswordResetToken usado = tokenFor(usuario, 1, true);
        PasswordResetToken vencido = tokenFor(usuario, -1, false);
        when(tokenRepo.findByToken("OK")).thenReturn(Optional.of(ok));
        when(tokenRepo.findByToken("USADO")).thenReturn(Optional.of(usado));
        when(tokenRepo.findByToken("VENCIDO")).thenReturn(Optional.of(vencido));
        when(tokenRepo.findByToken("GHOST")).thenReturn(Optional.empty());

        assertThat(service.isTokenValid("OK")).isTrue();
        assertThat(service.isTokenValid("USADO")).isFalse();
        assertThat(service.isTokenValid("VENCIDO")).isFalse();
        assertThat(service.isTokenValid("GHOST")).isFalse();
    }

    @Test
    @DisplayName("resetPassword: token ausente, senha curta e token invalido mapeiam errorCodes")
    void resetPassword_validacoes() {
        assertThatThrownBy(() -> service.resetPassword(" ", "novaSenha123", request))
            .isInstanceOf(UnauthorizedException.class)
            .extracting("errorCode").isEqualTo("RESET_TOKEN_MISSING");

        assertThatThrownBy(() -> service.resetPassword("TOK", "curta", request))
            .isInstanceOf(ValidationException.class)
            .extracting("errorCode").isEqualTo("PASSWORD_TOO_SHORT");

        when(tokenRepo.findByToken("TOK")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.resetPassword("TOK", "novaSenha123", request))
            .isInstanceOf(UnauthorizedException.class)
            .extracting("errorCode").isEqualTo("RESET_TOKEN_INVALID");
    }

    @Test
    @DisplayName("resetPassword: consome o token, troca a senha e zera lockout")
    void resetPassword_happyPath() {
        usuario.setFailedLoginAttempts(4);
        usuario.setLockedUntil(LocalDateTime.now().plusMinutes(10));
        PasswordResetToken prt = tokenFor(usuario, 1, false);
        when(tokenRepo.findByToken("TOK")).thenReturn(Optional.of(prt));
        when(passwordEncoder.encode("novaSenha123")).thenReturn("HASH");

        service.resetPassword("TOK", "novaSenha123", request);

        assertThat(usuario.getSenha()).isEqualTo("HASH");
        assertThat(usuario.getFailedLoginAttempts()).isZero();
        assertThat(usuario.getLockedUntil()).isNull();
        assertThat(prt.isUsado()).isTrue();
        verify(usuarioRepo).save(usuario);
        verify(tokenRepo).save(prt);
        verify(audit).log(any(), eq(usuario.getEmail()), any(), eq("PASSWORD_RESET"),
            eq("USUARIO"), any(), anyString(), anyString());
    }

    private static PasswordResetToken tokenFor(Usuario u, int horasAteExpirar, boolean usado) {
        PasswordResetToken t = new PasswordResetToken();
        t.setToken("TOK");
        t.setUsuario(u);
        t.setExpiresAt(LocalDateTime.now().plusHours(horasAteExpirar));
        t.setUsado(usado);
        return t;
    }
}

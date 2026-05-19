package br.com.cmms.cmms.service;

import br.com.cmms.cmms.Security.JwtService;
import br.com.cmms.cmms.Security.UserDetailsImpl;
import br.com.cmms.cmms.dto.TokenResponseDTO;
import br.com.cmms.cmms.exception.ForbiddenException;
import br.com.cmms.cmms.exception.UnauthorizedException;
import br.com.cmms.cmms.model.RefreshToken;
import br.com.cmms.cmms.model.Role;
import br.com.cmms.cmms.model.Usuario;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthService}. The Spring context is not loaded —
 * we wire Mockito mocks directly. Covers:
 * <ul>
 *   <li>happy-path login (resets failed counters, issues tokens, audits)</li>
 *   <li>bad credentials path (increments counter, throws Unauthorized)</li>
 *   <li>lockout enforcement (after threshold, ForbiddenException)</li>
 *   <li>unknown user (Unauthorized, no enumeration)</li>
 *   <li>refresh happy/missing-token cases</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock AuthenticationManager authenticationManager;
    @Mock JwtService jwtService;
    @Mock RefreshTokenService refreshTokenService;
    @Mock UsuarioRepository usuarioRepo;
    @Mock ConfiguracaoService config;
    @Mock AuditService audit;
    @Mock HttpServletRequest request;

    @InjectMocks AuthService authService;

    private Usuario activeUser;

    @BeforeEach
    void setUp() {
        activeUser = new Usuario();
        activeUser.setEmail("user@cmms.app");
        activeUser.setSenha("hashed");
        activeUser.setAtivo(true);
        activeUser.setFailedLoginAttempts(0);
        Role role = new Role("ROLE_TECNICO");
        activeUser.setRole(role);
        // remote addr is read by AuditService.getClientIp; default it to a stable value
        lenient().when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        lenient().when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    }

    @Test
    @DisplayName("login: usuário inexistente devolve BAD_CREDENTIALS sem revelar enumeração")
    void login_userNotFound_throwsUnauthorized() {
        when(usuarioRepo.findByEmail("ghost@cmms.app")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("ghost@cmms.app", "x", request))
            .isInstanceOf(UnauthorizedException.class)
            .extracting("errorCode").isEqualTo("BAD_CREDENTIALS");

        verify(audit, never()).log(anyString(), any(), anyString(), anyString(), any(), anyString(), anyString());
        verify(jwtService, never()).gerarToken(any());
    }

    @Test
    @DisplayName("login: conta bloqueada lança ForbiddenException ACCOUNT_LOCKED")
    void login_lockedAccount_throwsForbidden() {
        activeUser.setLockedUntil(LocalDateTime.now().plusMinutes(10));
        when(usuarioRepo.findByEmail(activeUser.getEmail())).thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> authService.login(activeUser.getEmail(), "x", request))
            .isInstanceOf(ForbiddenException.class)
            .extracting("errorCode").isEqualTo("ACCOUNT_LOCKED");

        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    @DisplayName("login: credencial OK emite tokens, zera contador e audita LOGIN")
    void login_happyPath_issuesTokensAndAudits() {
        when(usuarioRepo.findByEmail(activeUser.getEmail())).thenReturn(Optional.of(activeUser));

        UserDetailsImpl principal = new UserDetailsImpl(activeUser);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null);
        when(authenticationManager.authenticate(any())).thenReturn(auth);

        when(jwtService.gerarToken(activeUser)).thenReturn("ACCESS");
        RefreshToken rt = new RefreshToken();
        rt.setToken("REFRESH");
        when(refreshTokenService.criarRefreshToken(activeUser)).thenReturn(rt);

        TokenResponseDTO out = authService.login(activeUser.getEmail(), "ok", request);

        assertThat(out.getAccessToken()).isEqualTo("ACCESS");
        assertThat(out.getRefreshToken()).isEqualTo("REFRESH");
        assertThat(out.getRole()).isEqualTo("ROLE_TECNICO");

        ArgumentCaptor<Usuario> saved = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepo).save(saved.capture());
        assertThat(saved.getValue().getFailedLoginAttempts()).isZero();
        assertThat(saved.getValue().getLockedUntil()).isNull();
        assertThat(saved.getValue().getUltimoLogin()).isNotNull();

        verify(audit).log(eq(activeUser.getEmail()), any(), eq("LOGIN"),
                          eq("AUTH"), any(), anyString(), anyString());
    }

    @Test
    @DisplayName("login: BadCredentials incrementa contador sem bloquear ainda")
    void login_badCredentials_incrementsCounter() {
        when(usuarioRepo.findByEmail(activeUser.getEmail())).thenReturn(Optional.of(activeUser));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("nope"));
        when(config.getInt(eq("seguranca.lockout.tentativas"), anyInt())).thenReturn(5);

        assertThatThrownBy(() -> authService.login(activeUser.getEmail(), "wrong", request))
            .isInstanceOf(UnauthorizedException.class)
            .extracting("errorCode").isEqualTo("BAD_CREDENTIALS");

        ArgumentCaptor<Usuario> saved = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepo).save(saved.capture());
        assertThat(saved.getValue().getFailedLoginAttempts()).isEqualTo(1);
        assertThat(saved.getValue().getLockedUntil()).isNull();

        verify(audit).log(eq(activeUser.getEmail()), any(), eq("LOGIN_FAILED"),
                          eq("AUTH"), any(), anyString(), anyString());
    }

    @Test
    @DisplayName("login: na 5ª tentativa errada, conta vira locked")
    void login_badCredentials_atThreshold_locksAccount() {
        activeUser.setFailedLoginAttempts(4); // 4 falhas anteriores, esta será a 5ª
        when(usuarioRepo.findByEmail(activeUser.getEmail())).thenReturn(Optional.of(activeUser));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("nope"));
        when(config.getInt(eq("seguranca.lockout.tentativas"), anyInt())).thenReturn(5);
        when(config.getInt(eq("seguranca.lockout.minutos"), anyInt())).thenReturn(15);

        assertThatThrownBy(() -> authService.login(activeUser.getEmail(), "wrong", request))
            .isInstanceOf(UnauthorizedException.class);

        ArgumentCaptor<Usuario> saved = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepo).save(saved.capture());
        assertThat(saved.getValue().getFailedLoginAttempts()).isEqualTo(5);
        assertThat(saved.getValue().getLockedUntil()).isNotNull();
        assertThat(saved.getValue().getLockedUntil()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("refresh: token nulo ou em branco devolve REFRESH_TOKEN_MISSING")
    void refresh_blankToken_throws() {
        assertThatThrownBy(() -> authService.refresh(null))
            .isInstanceOf(UnauthorizedException.class)
            .extracting("errorCode").isEqualTo("REFRESH_TOKEN_MISSING");

        assertThatThrownBy(() -> authService.refresh("   "))
            .isInstanceOf(UnauthorizedException.class)
            .extracting("errorCode").isEqualTo("REFRESH_TOKEN_MISSING");

        verify(refreshTokenService, never()).rotacionar(anyString());
    }

    @Test
    @DisplayName("logout: revoga refresh tokens e audita")
    void logout_revokesAndAudits() {
        authService.logout(activeUser, request);

        verify(refreshTokenService).revogarTodos(activeUser);
        verify(audit).log(eq(activeUser.getEmail()), any(), eq("LOGOUT"),
                          eq("AUTH"), any(), anyString(), anyString());
    }

    @Test
    @DisplayName("logout: usuário nulo é no-op (idempotência)")
    void logout_nullUser_noop() {
        authService.logout(null, request);
        verify(refreshTokenService, never()).revogarTodos(any());
    }

    @Test
    @DisplayName("refresh: rotaciona o refresh token e devolve novo access")
    void refresh_happyPath_rotates() {
        RefreshToken rotated = new RefreshToken();
        rotated.setToken("REFRESH_NEW");
        rotated.setUsuario(activeUser);
        when(refreshTokenService.rotacionar("REFRESH_OLD")).thenReturn(rotated);
        when(jwtService.gerarToken(activeUser)).thenReturn("NEW_ACCESS");

        TokenResponseDTO out = authService.refresh("REFRESH_OLD");

        // Novo access E novo refresh token (rotação)
        assertThat(out.getAccessToken()).isEqualTo("NEW_ACCESS");
        assertThat(out.getRefreshToken()).isEqualTo("REFRESH_NEW");
        verify(refreshTokenService, times(1)).rotacionar("REFRESH_OLD");
        verify(jwtService, times(1)).gerarToken(activeUser);
    }

    // Local matcher helper to keep static imports tidy
    private static int anyInt() { return org.mockito.ArgumentMatchers.anyInt(); }
}

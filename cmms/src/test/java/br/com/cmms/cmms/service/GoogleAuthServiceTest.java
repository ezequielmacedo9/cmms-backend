package br.com.cmms.cmms.service;

import br.com.cmms.cmms.dto.TokenResponseDTO;
import br.com.cmms.cmms.exception.UnauthorizedException;
import br.com.cmms.cmms.model.Role;
import br.com.cmms.cmms.model.Usuario;
import br.com.cmms.cmms.repository.EmpresaRepository;
import br.com.cmms.cmms.repository.RoleRepository;
import br.com.cmms.cmms.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleAuthServiceTest {

    private static final String CLIENT_ID = "cmms-client.apps.googleusercontent.com";
    private static final String EMAIL = "ana@empresa.com";
    private static final String GOOGLE_ISS = "https://accounts.google.com";

    @Mock UsuarioRepository usuarioRepo;
    @Mock RoleRepository roleRepo;
    @Mock EmpresaRepository empresaRepo;
    @Mock AuthService authService;
    @Mock RestTemplate restTemplate;
    @Mock PasswordEncoder passwordEncoder;
    @Mock HttpServletRequest request;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TokenResponseDTO tokens = new TokenResponseDTO("ACCESS", "REFRESH", "ROLE_TECNICO", "Ana", 1L);
    private GoogleAuthService service;

    @BeforeEach
    void setUp() {
        service = serviceWithClientId(CLIENT_ID);
    }

    @Test
    @DisplayName("sem GOOGLE_CLIENT_ID configurado o login com Google fica desligado (fail-closed)")
    void loginWithGoogle_clientIdNotConfigured_failsClosed() {
        GoogleAuthService semClientId = serviceWithClientId("");

        assertThatThrownBy(() -> semClientId.loginWithGoogle("token", null, request))
            .isInstanceOf(UnauthorizedException.class)
            .extracting("errorCode").isEqualTo("GOOGLE_LOGIN_DISABLED");
        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("token emitido para outro app (aud diferente) e recusado")
    void loginWithGoogle_tokenFromAnotherApp_isRejected() {
        googleResponds("outro-app.apps.googleusercontent.com", GOOGLE_ISS, "true", "111");

        assertThatThrownBy(() -> service.loginWithGoogle("token", null, request))
            .isInstanceOf(UnauthorizedException.class)
            .extracting("errorCode").isEqualTo("GOOGLE_TOKEN_INVALID");
        verifyNoInteractions(usuarioRepo);
    }

    @Test
    @DisplayName("emissor que nao e o Google e recusado")
    void loginWithGoogle_wrongIssuer_isRejected() {
        googleResponds(CLIENT_ID, "https://evil.example", "true", "111");

        assertThatThrownBy(() -> service.loginWithGoogle("token", null, request))
            .isInstanceOf(UnauthorizedException.class)
            .extracting("errorCode").isEqualTo("GOOGLE_TOKEN_INVALID");
    }

    @Test
    @DisplayName("e-mail nao verificado no Google e recusado")
    void loginWithGoogle_emailNotVerified_isRejected() {
        googleResponds(CLIENT_ID, "accounts.google.com", "false", "111");

        assertThatThrownBy(() -> service.loginWithGoogle("token", null, request))
            .isInstanceOf(UnauthorizedException.class)
            .extracting("errorCode").isEqualTo("GOOGLE_EMAIL_NOT_VERIFIED");
        verifyNoInteractions(usuarioRepo);
    }

    @Test
    @DisplayName("conta ja vinculada a outro sub do Google e recusada")
    void loginWithGoogle_subDiffersFromLinkedAccount_isRejected() {
        googleResponds(CLIENT_ID, GOOGLE_ISS, "true", "222");
        when(usuarioRepo.findByEmail(EMAIL)).thenReturn(Optional.of(usuario("111")));

        assertThatThrownBy(() -> service.loginWithGoogle("token", null, request))
            .isInstanceOf(UnauthorizedException.class)
            .extracting("errorCode").isEqualTo("GOOGLE_ACCOUNT_MISMATCH");
        verify(authService, never()).concluirLogin(any(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("usuario desativado nao entra pelo Google")
    void loginWithGoogle_disabledUser_isRejected() {
        googleResponds(CLIENT_ID, GOOGLE_ISS, "true", "111");
        Usuario desativado = usuario("111");
        desativado.setAtivo(false);
        when(usuarioRepo.findByEmail(EMAIL)).thenReturn(Optional.of(desativado));

        assertThatThrownBy(() -> service.loginWithGoogle("token", null, request))
            .isInstanceOf(UnauthorizedException.class)
            .extracting("errorCode").isEqualTo("ACCOUNT_DISABLED");
    }

    @Test
    @DisplayName("usuario com 2FA: sem codigo recebe TOTP_REQUIRED e nenhum token e emitido")
    void loginWithGoogle_secondFactorMissing_doesNotIssueTokens() {
        googleResponds(CLIENT_ID, GOOGLE_ISS, "true", "111");
        Usuario comTotp = usuario(null);
        comTotp.setTotpEnabled(true);
        when(usuarioRepo.findByEmail(EMAIL)).thenReturn(Optional.of(comTotp));
        doThrow(new UnauthorizedException("TOTP_REQUIRED", "Informe o código."))
            .when(authService).verificarSegundoFator(comTotp, null, request);

        assertThatThrownBy(() -> service.loginWithGoogle("token", null, request))
            .isInstanceOf(UnauthorizedException.class)
            .extracting("errorCode").isEqualTo("TOTP_REQUIRED");
        verify(authService, never()).concluirLogin(any(), anyString(), anyString(), any());
        assertThat(comTotp.getGoogleId()).isNull();
    }

    @Test
    @DisplayName("token valido de usuario existente: aplica bloqueio e 2FA, vincula o sub e emite tokens")
    void loginWithGoogle_validTokenExistingUser_linksAndIssuesTokens() {
        googleResponds(CLIENT_ID, GOOGLE_ISS, "true", "111");
        Usuario existente = usuario(null);
        when(usuarioRepo.findByEmail(EMAIL)).thenReturn(Optional.of(existente));
        when(authService.concluirLogin(existente, "LOGIN_GOOGLE", "Login via Google", request)).thenReturn(tokens);

        TokenResponseDTO out = service.loginWithGoogle("token", "123456", request);

        assertThat(out).isSameAs(tokens);
        assertThat(existente.getGoogleId()).isEqualTo("111");
        verify(authService).assertNaoBloqueada(existente);
        verify(authService).verificarSegundoFator(existente, "123456", request);
    }

    @Test
    @DisplayName("e-mail novo cria a empresa e o usuario ADMIN vinculado ao sub")
    void loginWithGoogle_newEmail_provisionsEmpresaAndAdmin() {
        googleResponds(CLIENT_ID, GOOGLE_ISS, "true", "999");
        when(usuarioRepo.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(roleRepo.findByNome("ROLE_ADMIN")).thenReturn(Optional.of(new Role("ROLE_ADMIN")));
        when(empresaRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(usuarioRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(authService.concluirLogin(any(), eq("LOGIN_GOOGLE"), anyString(), eq(request))).thenReturn(tokens);

        service.loginWithGoogle("token", null, request);

        ArgumentCaptor<Usuario> novo = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepo).save(novo.capture());
        assertThat(novo.getValue().getEmail()).isEqualTo(EMAIL);
        assertThat(novo.getValue().getGoogleId()).isEqualTo("999");
        assertThat(novo.getValue().getRole().getNome()).isEqualTo("ROLE_ADMIN");
    }

    private GoogleAuthService serviceWithClientId(String clientId) {
        return new GoogleAuthService(usuarioRepo, roleRepo, empresaRepo, authService,
            restTemplate, objectMapper, passwordEncoder, clientId);
    }

    private void googleResponds(String aud, String iss, String emailVerified, String sub) {
        String json = objectMapper.createObjectNode()
            .put("aud", aud)
            .put("iss", iss)
            .put("email", EMAIL)
            .put("email_verified", emailVerified)
            .put("sub", sub)
            .put("name", "Ana")
            .toString();
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(json);
    }

    private static Usuario usuario(String googleId) {
        Usuario u = new Usuario();
        u.setEmail(EMAIL);
        u.setAtivo(true);
        u.setGoogleId(googleId);
        u.setRole(new Role("ROLE_TECNICO"));
        return u;
    }
}

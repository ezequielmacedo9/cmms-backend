package br.com.cmms.cmms.service;

import br.com.cmms.cmms.exception.UnauthorizedException;
import br.com.cmms.cmms.model.RefreshToken;
import br.com.cmms.cmms.model.Usuario;
import br.com.cmms.cmms.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit tests for {@link RefreshTokenService} (issue, rotate, validate, revoke). */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock RefreshTokenRepository repository;
    @InjectMocks RefreshTokenService service;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", 3L);
        usuario.setEmail("user@cmms.app");
        // repository.save devolve o proprio argumento
        lenient().when(repository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("criarRefreshToken: apaga tokens antigos e emite novo com 7 dias")
    void criar_substituiAnterior() {
        RefreshToken rt = service.criarRefreshToken(usuario);

        verify(repository).deleteByUsuarioId(3L);
        assertThat(rt.getToken()).isNotBlank();
        assertThat(rt.getExpiracao()).isAfter(Instant.now().plus(6, ChronoUnit.DAYS));
    }

    @Test
    @DisplayName("rotacionar: token valido e consumido e um novo e emitido")
    void rotacionar_trocaToken() {
        RefreshToken atual = new RefreshToken();
        atual.setToken("OLD");
        atual.setUsuario(usuario);
        atual.setExpiracao(Instant.now().plus(1, ChronoUnit.DAYS));
        when(repository.findByToken("OLD")).thenReturn(Optional.of(atual));

        RefreshToken novo = service.rotacionar("OLD");

        verify(repository).delete(atual);
        assertThat(novo.getToken()).isNotEqualTo("OLD");
        assertThat(novo.getUsuario()).isSameAs(usuario);
    }

    @Test
    @DisplayName("validar: ausente/desconhecido/expirado devolvem os errorCodes corretos")
    void validar_errosMapeados() {
        assertThatThrownBy(() -> service.validar("  "))
            .isInstanceOf(UnauthorizedException.class)
            .extracting("errorCode").isEqualTo("REFRESH_TOKEN_MISSING");

        when(repository.findByToken("GHOST")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.validar("GHOST"))
            .isInstanceOf(UnauthorizedException.class)
            .extracting("errorCode").isEqualTo("REFRESH_TOKEN_INVALID");

        RefreshToken vencido = new RefreshToken();
        vencido.setToken("EXPIRED");
        vencido.setUsuario(usuario);
        vencido.setExpiracao(Instant.now().minusSeconds(60));
        when(repository.findByToken("EXPIRED")).thenReturn(Optional.of(vencido));
        assertThatThrownBy(() -> service.validar("EXPIRED"))
            .isInstanceOf(UnauthorizedException.class)
            .extracting("errorCode").isEqualTo("REFRESH_TOKEN_EXPIRED");
        verify(repository).delete(vencido);
    }

    @Test
    @DisplayName("revogarTodos: apaga todos os tokens do usuario")
    void revogarTodos_apaga() {
        service.revogarTodos(usuario);
        verify(repository).deleteByUsuarioId(3L);
    }
}

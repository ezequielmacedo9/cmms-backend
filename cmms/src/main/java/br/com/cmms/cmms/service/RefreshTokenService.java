package br.com.cmms.cmms.service;

import br.com.cmms.cmms.model.RefreshToken;
import br.com.cmms.cmms.model.Usuario;
import br.com.cmms.cmms.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Transactional
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repository;

    public RefreshTokenService(RefreshTokenRepository repository) {
        this.repository = repository;
    }


    @Transactional
    public RefreshToken criarRefreshToken(Usuario usuario) {

        repository.deleteByUsuarioId(usuario.getId());
        repository.flush();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUsuario(usuario);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiracao(Instant.now().plus(7, ChronoUnit.DAYS));

        return repository.save(refreshToken);
    }

    public RefreshToken validar(String token) {
        if (token == null || token.isBlank()) {
            throw new RuntimeException("Refresh token inválido");
        }
        RefreshToken refreshToken = repository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Refresh token inválido"));

        if (refreshToken.getExpiracao().isBefore(Instant.now())) {
            repository.delete(refreshToken);
            throw new RuntimeException("Refresh token expirado");
        }

        return refreshToken;
    }
}

package br.com.cmms.cmms.repository;

import br.com.cmms.cmms.model.RefreshToken;
import br.com.cmms.cmms.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository
        extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByUsuario(Usuario usuario);
}

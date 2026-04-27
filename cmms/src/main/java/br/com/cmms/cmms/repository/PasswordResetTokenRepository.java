package br.com.cmms.cmms.repository;

import br.com.cmms.cmms.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);

    @Modifying @Transactional
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :now OR t.usado = true")
    void deleteExpiredAndUsed(LocalDateTime now);
}

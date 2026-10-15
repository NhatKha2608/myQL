package vn.edu.qlhs.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.qlhs.auth.entity.PasswordResetTokenEntity;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, Long> {
    Optional<PasswordResetTokenEntity> findByTokenHashAndUsedAtIsNull(String tokenHash);
}

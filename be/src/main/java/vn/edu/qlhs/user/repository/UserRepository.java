package vn.edu.qlhs.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.qlhs.user.entity.UserEntity;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmailAndDeletedFalse(String email);

    Optional<UserEntity> findByUsernameAndDeletedFalse(String username);

    Optional<UserEntity> findByIdAndDeletedFalse(Long id);

    boolean existsByEmailAndDeletedFalse(String email);

    boolean existsByUsernameAndDeletedFalse(String username);
}

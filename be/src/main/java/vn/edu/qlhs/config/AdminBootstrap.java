package vn.edu.qlhs.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import vn.edu.qlhs.common.enums.UserRole;
import vn.edu.qlhs.common.enums.UserStatus;
import vn.edu.qlhs.user.entity.UserEntity;
import vn.edu.qlhs.user.repository.UserRepository;

/**
 * Tạo tài khoản admin lần đầu trên production khi database còn trống user đó.
 * Không ghi mật khẩu ra log. Xóa APP_ADMIN_PASSWORD trên Render sau khi đăng nhập được.
 */
@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:}")
    private String username;

    @Value("${app.admin.password:}")
    private String password;

    @Value("${app.admin.email:}")
    private String email;

    @Value("${app.admin.full-name:Quản trị hệ thống}")
    private String fullName;

    @Override
    public void run(ApplicationArguments args) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            log.warn("Chưa đặt APP_ADMIN_USERNAME và APP_ADMIN_PASSWORD. Bỏ qua tạo admin.");
            return;
        }
        if (password.length() < 8 || "password".equals(password)) {
            log.error("APP_ADMIN_PASSWORD phải dài ít nhất 8 ký tự và không được là mật khẩu demo. Không tạo admin.");
            return;
        }
        if (userRepository.existsByUsernameAndDeletedFalse(username.trim())) {
            log.info("Tài khoản '{}' đã tồn tại. Bỏ qua tạo admin.", username.trim());
            return;
        }

        String resolvedEmail = (email == null || email.isBlank())
                ? username.trim() + "@qlhs.local"
                : email.trim();
        if (userRepository.existsByEmailAndDeletedFalse(resolvedEmail)) {
            log.error("Email {} đã được dùng. Không tạo admin.", resolvedEmail);
            return;
        }

        UserEntity admin = UserEntity.builder()
                .fullName(fullName == null || fullName.isBlank() ? "Quản trị hệ thống" : fullName.trim())
                .email(resolvedEmail)
                .username(username.trim())
                .passwordHash(passwordEncoder.encode(password))
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(admin);
        log.info("Đã tạo tài khoản admin '{}'. Đăng nhập rồi đổi mật khẩu, sau đó xóa APP_ADMIN_PASSWORD trên Render.", username.trim());
    }
}

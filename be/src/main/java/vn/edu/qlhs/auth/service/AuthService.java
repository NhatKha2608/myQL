package vn.edu.qlhs.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.qlhs.audit.service.AuditLogService;
import vn.edu.qlhs.auth.dto.*;
import vn.edu.qlhs.auth.entity.PasswordResetTokenEntity;
import vn.edu.qlhs.auth.entity.RefreshTokenEntity;
import vn.edu.qlhs.auth.repository.PasswordResetTokenRepository;
import vn.edu.qlhs.auth.repository.RefreshTokenRepository;
import vn.edu.qlhs.common.exception.BusinessException;
import vn.edu.qlhs.common.exception.ResourceNotFoundException;
import vn.edu.qlhs.config.QlhsProperties;
import vn.edu.qlhs.security.JwtService;
import vn.edu.qlhs.security.SecurityUtils;
import vn.edu.qlhs.security.UserPrincipal;
import vn.edu.qlhs.user.entity.UserEntity;
import vn.edu.qlhs.user.repository.UserRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final AuditLogService auditLogService;
    private final LoginAttemptService loginAttemptService;
    private final QlhsProperties properties;
    private final JavaMailSender mailSender;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String key = request.getUsernameOrEmail().toLowerCase();
        loginAttemptService.checkAllowed(key);
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsernameOrEmail(), request.getPassword()));
        } catch (Exception ex) {
            loginAttemptService.onFailure(key);
            auditLogService.log(null, "LOGIN", "USER", null, "Đăng nhập thất bại: " + key, false);
            throw ex;
        }
        loginAttemptService.onSuccess(key);
        UserEntity user = userRepository.findByEmailAndDeletedFalse(request.getUsernameOrEmail())
                .or(() -> userRepository.findByUsernameAndDeletedFalse(request.getUsernameOrEmail()))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        UserPrincipal principal = new UserPrincipal(user);
        String accessToken = jwtService.generateAccessToken(principal);
        String refreshToken = jwtService.generateRefreshToken(principal);
        saveRefreshToken(user.getId(), refreshToken);
        auditLogService.log(user.getId(), "LOGIN", "USER", user.getId(), "Đăng nhập thành công", true);
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(toProfile(user))
                .build();
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(hashToken(refreshToken)).ifPresent(token -> {
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
        });
        UserPrincipal principal = SecurityUtils.currentUser();
        auditLogService.log(principal.getId(), "LOGOUT", "USER", principal.getId(), "Đăng xuất", true);
    }

    public UserProfileResponse me() {
        UserPrincipal principal = SecurityUtils.currentUser();
        UserEntity user = userRepository.findByIdAndDeletedFalse(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        return toProfile(user);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        UserPrincipal principal = SecurityUtils.currentUser();
        UserEntity user = userRepository.findByIdAndDeletedFalse(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessException("Mật khẩu hiện tại không đúng");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        auditLogService.log(user.getId(), "CHANGE_PASSWORD", "USER", user.getId(), "Đổi mật khẩu", true);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmailAndDeletedFalse(request.getEmail()).ifPresent(user -> {
            String rawToken = generateSecureToken();
            passwordResetTokenRepository.save(PasswordResetTokenEntity.builder()
                    .userId(user.getId())
                    .tokenHash(hashToken(rawToken))
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build());
            String link = properties.getFrontend().getResetPasswordUrl() + "?token=" + rawToken;
            if (!mailEnabled) {
                log.info("Mail tắt (app.mail.enabled=false). Link khôi phục cho {}: {}", user.getEmail(), link);
            } else {
                try {
                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setFrom(properties.getMail().getFrom());
                    message.setTo(user.getEmail());
                    message.setSubject("Khôi phục mật khẩu QLHS");
                    message.setText("Bạn có thể đặt lại mật khẩu tại: " + link + "\nLiên kết có hiệu lực 1 giờ.");
                    mailSender.send(message);
                } catch (Exception ex) {
                    log.warn("Gửi email khôi phục mật khẩu thất bại: {}", ex.getMessage());
                }
            }
            auditLogService.log(user.getId(), "FORGOT_PASSWORD", "USER", user.getId(), "Yêu cầu khôi phục mật khẩu", true);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetTokenEntity token = passwordResetTokenRepository
                .findByTokenHashAndUsedAtIsNull(hashToken(request.getToken()))
                .filter(t -> t.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new BusinessException("Token không hợp lệ hoặc đã hết hạn"));
        UserEntity user = userRepository.findByIdAndDeletedFalse(token.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        token.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(token);
        auditLogService.log(user.getId(), "RESET_PASSWORD", "USER", user.getId(), "Đặt lại mật khẩu", true);
    }

    private void saveRefreshToken(Long userId, String refreshToken) {
        refreshTokenRepository.save(RefreshTokenEntity.builder()
                .userId(userId)
                .tokenHash(hashToken(refreshToken))
                .expiresAt(Instant.now().plusMillis(properties.getJwt().getRefreshTokenExpirationMs()))
                .build());
    }

    private UserProfileResponse toProfile(UserEntity user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole())
                .status(user.getStatus())
                .build();
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

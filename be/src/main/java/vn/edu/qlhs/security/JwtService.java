package vn.edu.qlhs.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.qlhs.config.QlhsProperties;
import vn.edu.qlhs.common.enums.UserRole;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final QlhsProperties properties;

    public String generateAccessToken(UserPrincipal principal) {
        Instant now = Instant.now();
        Instant exp = now.plusMillis(properties.getJwt().getAccessTokenExpirationMs());
        return buildToken(Map.of(
                "typ", "access",
                "uid", principal.getId(),
                "role", principal.getRole().name()
        ), principal.getEmail(), exp);
    }

    public String generateRefreshToken(UserPrincipal principal) {
        Instant exp = Instant.now().plusMillis(properties.getJwt().getRefreshTokenExpirationMs());
        return buildToken(Map.of("typ", "refresh", "uid", principal.getId()), principal.getEmail(), exp);
    }

    private String buildToken(Map<String, Object> claims, String subject, Instant expiration) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(expiration))
                .signWith(signingKey())
                .compact();
    }

    public String extractSubject(String token) {
        return parseClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        Object uid = parseClaims(token).get("uid");
        if (uid instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalArgumentException("Token không hợp lệ");
    }

    public UserRole extractRole(String token) {
        Object role = parseClaims(token).get("role");
        if (role == null) {
            return null;
        }
        return UserRole.valueOf(role.toString());
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            return "refresh".equals(parseClaims(token).get("typ"));
        } catch (Exception ex) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        String secret = properties.getJwt().getSecret();
        byte[] keyBytes;
        if (secret.length() >= 32) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        } else {
            keyBytes = Decoders.BASE64.decode(secret);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

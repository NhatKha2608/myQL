package vn.edu.qlhs.auth.dto;

import lombok.Builder;
import lombok.Data;
import vn.edu.qlhs.common.enums.UserRole;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private UserProfileResponse user;
}

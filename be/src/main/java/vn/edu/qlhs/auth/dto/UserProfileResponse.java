package vn.edu.qlhs.auth.dto;

import lombok.Builder;
import lombok.Data;
import vn.edu.qlhs.common.enums.UserRole;
import vn.edu.qlhs.common.enums.UserStatus;

@Data
@Builder
public class UserProfileResponse {
    private Long id;
    private String fullName;
    private String email;
    private String username;
    private UserRole role;
    private UserStatus status;
}

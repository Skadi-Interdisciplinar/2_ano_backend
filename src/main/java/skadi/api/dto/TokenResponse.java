package skadi.api.dto;

import java.sql.Timestamp;

public record TokenResponse(
        String token,
        Long expirationTime,
        Timestamp validUntil,
        UserResponse user
) {
    public TokenResponse(String token, Long expirationTime, Timestamp validUntil) {
        this(token, expirationTime, validUntil, null);
    }
}

package skadi.api.dto;

import java.sql.Timestamp;

public record TokenResponse(String token, Long expirationTime, Timestamp validUntil) {
}

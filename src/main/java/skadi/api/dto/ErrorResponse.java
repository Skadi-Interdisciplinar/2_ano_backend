package skadi.api.dto;


import java.sql.Timestamp;

public record ErrorResponse(Integer statusCode, String message, Timestamp now) {
}

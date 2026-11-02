package skadi.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record LoginRequest(
        String username,
        @JsonAlias({"senha"}) String password
) {
}

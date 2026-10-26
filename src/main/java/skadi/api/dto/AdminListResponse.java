package skadi.api.dto;

import java.util.List;

public record AdminListResponse(
        Integer total,
        List<UserResponse> usuarios
) {
}

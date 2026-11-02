package skadi.api.dto;

public record AdminPerCdDto(
        String cdId,
        String cdNome,
        long totalAdmins
) {
}

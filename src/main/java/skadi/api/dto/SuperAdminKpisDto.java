package skadi.api.dto;

public record SuperAdminKpisDto(
        long totalAdministradores,
        long totalCds,
        long chamadosAbertos,
        long chamadosRegistrados,
        long chamadosEmAtendimento,
        long chamadosAtendidos
) {
}

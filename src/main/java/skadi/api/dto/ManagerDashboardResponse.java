package skadi.api.dto;

import java.util.List;

public record ManagerDashboardResponse(
        Long cdId,
        String cdName,
        int criticosCount,
        int slaVencidoCount,
        int foraFaixaCount,
        int semResponsavelCount,
        int totalCamaras,
        int naFaixaCamaras,
        int foraFaixaCamaras,
        int offlineCamaras,
        int semDadosCamaras,
        List<PriorityAlertDto> priorities,
        List<Float> trendReadings
) {
}

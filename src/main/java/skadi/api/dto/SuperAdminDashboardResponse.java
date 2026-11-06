package skadi.api.dto;

import skadi.api.enums.SupportCategory;
import skadi.api.enums.SupportStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record SuperAdminDashboardResponse(
        SuperAdminKpisDto kpis,
        Map<SupportStatus, Long> chamadosPorSituacao,
        Map<SupportCategory, Long> chamadosPorCategoria,
        List<AdminPerCdDto> administradoresPorCd,
        List<RecentSupportTicketDto> chamadosRecentes,
        LocalDateTime ultimaAtualizacao
) {
}

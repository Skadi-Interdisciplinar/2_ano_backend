package skadi.api.dto;

import java.util.List;

public record EmployeeDashboardResponse(
        Long userId,
        String userName,
        String cdName,
        int atribuidosCount,
        int criticosCount,
        int emAtendimentoCount,
        int slaVencidoCount,
        int proximosVencimentoCount,
        PriorityAlertDto nextAction,
        List<PriorityAlertDto> queue
) {
}

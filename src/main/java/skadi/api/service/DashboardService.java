package skadi.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skadi.api.dto.EmployeeDashboardResponse;
import skadi.api.dto.ManagerDashboardResponse;
import skadi.api.dto.PriorityAlertDto;
import skadi.api.enums.AlertStatus;
import skadi.api.model.Alert;
import skadi.api.model.ColdRoom;
import skadi.api.model.DistributionCenter;
import skadi.api.model.User;
import skadi.api.repository.AlertRepository;
import skadi.api.repository.ColdRoomRepository;
import skadi.api.repository.DistributionCenterRepository;
import skadi.api.repository.TemperatureReadingRepository;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AlertRepository alertRepository;
    private final ColdRoomRepository coldRoomRepository;
    private final TemperatureReadingRepository readingRepository;
    private final DistributionCenterRepository cdRepository;
    private final DistributionCenterAccessService accessService;

    @Transactional(readOnly = true)
    public ManagerDashboardResponse getManagerDashboard(User user) {
        Long cdId = user.getCdId() != null ? user.getCdId().longValue() : 1L;
        String cdName = cdRepository.findById(cdId)
                .map(DistributionCenter::getName)
                .orElse("CD Manaus - Unidade 20");

        List<ColdRoom> coldRooms = coldRoomRepository
                .findAllByDistributionCenter_IdInOrderByLocationAscIdAsc(List.of(cdId));
        if (coldRooms.isEmpty()) {
            coldRooms = coldRoomRepository.findAll();
        }

        LocalDateTime now = LocalDateTime.now();
        int totalCamaras = coldRooms.size();
        int naFaixaCamaras = 0;
        int foraFaixaCamaras = 0;
        int offlineCamaras = 0;
        int semDadosCamaras = 0;

        Map<Long, Object[]> latestReadings = new HashMap<>();

        for (ColdRoom cr : coldRooms) {
            List<Object[]> rows = readingRepository.findCurrentByColdRoomIdAndCd(cr.getId(), Math.toIntExact(cdId), true);
            if (rows.isEmpty()) {
                semDadosCamaras++;
            } else {
                Object[] row = rows.get(0);
                latestReadings.put(cr.getId(), row);

                BigDecimal temp = row[4] instanceof BigDecimal ? (BigDecimal) row[4] : null;
                LocalDateTime measuredAt = row[5] instanceof LocalDateTime ? (LocalDateTime) row[5] : null;

                if (temp == null) {
                    semDadosCamaras++;
                } else if (measuredAt != null && Math.abs(Duration.between(measuredAt, now).toMinutes()) > 10) {
                    offlineCamaras++;
                } else {
                    double t = temp.doubleValue();
                    double min = cr.getMinimumTemperature() != null ? cr.getMinimumTemperature().doubleValue() : -2.0;
                    double max = cr.getMaximumTemperature() != null ? cr.getMaximumTemperature().doubleValue() : 4.0;
                    if (t >= min && t <= max) {
                        naFaixaCamaras++;
                    } else {
                        foraFaixaCamaras++;
                    }
                }
            }
        }

        // Alertas abertos
        List<Alert> allAlerts = alertRepository.findAllByOrderByCreatedAtDesc();
        List<Alert> openAlerts = allAlerts.stream()
                .filter(a -> a.getStatus() != AlertStatus.RESOLVIDO)
                .filter(a -> {
                    Optional<Long> alertCd = alertRepository.findDistributionCenterIdByAlertId(a.getId());
                    return alertCd.isEmpty() || accessService.canRead(user, alertCd.get());
                })
                .toList();

        int criticosCount = 0;
        int slaVencidoCount = 0;
        int semResponsavelCount = 0;

        List<PriorityAlertDto> priorityDtos = new ArrayList<>();

        for (Alert a : openAlerts) {
            boolean isCritical = isCritical(a);
            if (isCritical) {
                criticosCount++;
            }

            SlaCalculation sla = calculateSla(a, now);
            if ("SLA_VENCIDO".equals(sla.status)) {
                slaVencidoCount++;
            }

            if (a.getCurrentLevel() == null || "OPERADOR".equalsIgnoreCase(a.getCurrentLevel().name())) {
                semResponsavelCount++;
            }

            ColdRoom cr = coldRooms.stream()
                    .filter(c -> c.getId().equals(a.getColdRoomId().longValue()))
                    .findFirst()
                    .orElse(null);

            Object[] readingRow = cr != null ? latestReadings.get(cr.getId()) : null;
            BigDecimal currentTemp = readingRow != null && readingRow[4] instanceof BigDecimal
                    ? (BigDecimal) readingRow[4] : null;

            priorityDtos.add(new PriorityAlertDto(
                    a.getId(),
                    cr != null ? cr.getId() : a.getColdRoomId().longValue(),
                    cr != null ? cr.getLocation() : "Câmara " + a.getColdRoomId(),
                    currentTemp,
                    cr != null ? cr.getMinimumTemperature() : null,
                    cr != null ? cr.getMaximumTemperature() : null,
                    a.getSeverityLevel() != null ? a.getSeverityLevel().name() : "ATENCAO",
                    a.getStatus() != null ? a.getStatus().name() : "ATIVO",
                    a.getCurrentLevel() != null ? a.getCurrentLevel().name() : "OPERADOR",
                    a.getCreatedAt(),
                    sla.remainingMinutes,
                    sla.percentConsumed,
                    sla.status,
                    a.getCurrentLevel() != null ? "Nível " + a.getCurrentLevel().name() : "Não atribuído"
            ));
        }

        // Ordenação por prioridade: Crítico -> SLA vencido -> SLA próximo -> mais antigo
        sortPriorities(priorityDtos);

        List<PriorityAlertDto> top3 = priorityDtos.stream().limit(3).toList();

        // Leituras de tendência térmica da primeira câmara com prioridade ou #1
        List<Float> trendReadings = new ArrayList<>();
        Long targetRoom = !priorityDtos.isEmpty() ? priorityDtos.get(0).coldRoomId() : 1L;
        List<Object[]> roomReadings = readingRepository.findCurrentByColdRoomIdAndCd(targetRoom, Math.toIntExact(cdId), true);
        for (Object[] r : roomReadings) {
            if (r[4] instanceof BigDecimal b) {
                trendReadings.add(b.floatValue());
            }
        }

        return new ManagerDashboardResponse(
                cdId,
                cdName,
                criticosCount,
                slaVencidoCount,
                foraFaixaCamaras,
                semResponsavelCount,
                totalCamaras,
                naFaixaCamaras,
                foraFaixaCamaras,
                offlineCamaras,
                semDadosCamaras,
                top3,
                trendReadings
        );
    }

    @Transactional(readOnly = true)
    public EmployeeDashboardResponse getEmployeeDashboard(User user) {
        Long cdId = user.getCdId() != null ? user.getCdId().longValue() : 1L;
        String cdName = cdRepository.findById(cdId)
                .map(DistributionCenter::getName)
                .orElse("CD Manaus - Unidade 20");

        List<ColdRoom> coldRooms = coldRoomRepository
                .findAllByDistributionCenter_IdInOrderByLocationAscIdAsc(List.of(cdId));
        if (coldRooms.isEmpty()) {
            coldRooms = coldRoomRepository.findAll();
        }

        LocalDateTime now = LocalDateTime.now();
        List<Alert> allAlerts = alertRepository.findAllByOrderByCreatedAtDesc();

        List<Alert> myAlerts = allAlerts.stream()
                .filter(a -> a.getStatus() != AlertStatus.RESOLVIDO)
                .filter(a -> {
                    Optional<Long> alertCd = alertRepository.findDistributionCenterIdByAlertId(a.getId());
                    return alertCd.isEmpty() || accessService.canRead(user, alertCd.get());
                })
                .toList();

        int criticosCount = 0;
        int emAtendimentoCount = 0;
        int slaVencidoCount = 0;
        int proximosVencimentoCount = 0;

        List<PriorityAlertDto> queue = new ArrayList<>();

        for (Alert a : myAlerts) {
            boolean isCritical = isCritical(a);
            if (isCritical) criticosCount++;

            if (a.getStatus() != null && "RECONHECIDO".equalsIgnoreCase(a.getStatus().name())) {
                emAtendimentoCount++;
            }

            SlaCalculation sla = calculateSla(a, now);
            if ("SLA_VENCIDO".equals(sla.status)) {
                slaVencidoCount++;
            } else if ("PROXIMO_VENCIMENTO".equals(sla.status)) {
                proximosVencimentoCount++;
            }

            ColdRoom cr = coldRooms.stream()
                    .filter(c -> c.getId().equals(a.getColdRoomId().longValue()))
                    .findFirst()
                    .orElse(null);

            List<Object[]> rows = readingRepository.findCurrentByColdRoomIdAndCd(
                    a.getColdRoomId().longValue(), Math.toIntExact(cdId), true);
            BigDecimal currentTemp = (!rows.isEmpty() && rows.get(0)[4] instanceof BigDecimal b) ? b : null;

            queue.add(new PriorityAlertDto(
                    a.getId(),
                    cr != null ? cr.getId() : a.getColdRoomId().longValue(),
                    cr != null ? cr.getLocation() : "Câmara " + a.getColdRoomId(),
                    currentTemp,
                    cr != null ? cr.getMinimumTemperature() : null,
                    cr != null ? cr.getMaximumTemperature() : null,
                    a.getSeverityLevel() != null ? a.getSeverityLevel().name() : "ATENCAO",
                    a.getStatus() != null ? a.getStatus().name() : "ATIVO",
                    a.getCurrentLevel() != null ? a.getCurrentLevel().name() : "OPERADOR",
                    a.getCreatedAt(),
                    sla.remainingMinutes,
                    sla.percentConsumed,
                    sla.status,
                    user.getNome() != null ? user.getNome() : "Operador"
            ));
        }

        sortPriorities(queue);

        PriorityAlertDto nextAction = !queue.isEmpty() ? queue.get(0) : null;

        return new EmployeeDashboardResponse(
                user.getId(),
                user.getNome(),
                cdName,
                queue.size(),
                criticosCount,
                emAtendimentoCount,
                slaVencidoCount,
                proximosVencimentoCount,
                nextAction,
                queue
        );
    }

    private void sortPriorities(List<PriorityAlertDto> list) {
        list.sort((p1, p2) -> {
            boolean c1 = p1.severityLevel() != null && p1.severityLevel().contains("CRITIC");
            boolean c2 = p2.severityLevel() != null && p2.severityLevel().contains("CRITIC");
            if (c1 != c2) return c1 ? -1 : 1;

            boolean v1 = "SLA_VENCIDO".equals(p1.slaStatus());
            boolean v2 = "SLA_VENCIDO".equals(p2.slaStatus());
            if (v1 != v2) return v1 ? -1 : 1;

            boolean px1 = "PROXIMO_VENCIMENTO".equals(p1.slaStatus());
            boolean px2 = "PROXIMO_VENCIMENTO".equals(p2.slaStatus());
            if (px1 != px2) return px1 ? -1 : 1;

            return p1.createdAt() != null && p2.createdAt() != null
                    ? p1.createdAt().compareTo(p2.createdAt()) : 0;
        });
    }

    private boolean isCritical(Alert alert) {
        if (alert == null || alert.getSeverityLevel() == null) return false;
        String s = alert.getSeverityLevel().name().toUpperCase();
        return s.contains("CRITIC") || s.contains("URGENT");
    }

    private SlaCalculation calculateSla(Alert alert, LocalDateTime now) {
        if (alert == null || alert.getCreatedAt() == null || alert.getReferenceShelfLifeHours() == null) {
            return new SlaCalculation(null, null, "SEM_REFERENCIA");
        }

        try {
            long elapsedMinutes = Math.abs(Duration.between(alert.getCreatedAt(), now).toMinutes());
            long totalAllowedMinutes = (long) (alert.getReferenceShelfLifeHours().doubleValue() * 60);

            if (totalAllowedMinutes <= 0) {
                return new SlaCalculation(null, null, "SEM_REFERENCIA");
            }

            long remaining = totalAllowedMinutes - elapsedMinutes;
            double percent = (elapsedMinutes * 100.0) / totalAllowedMinutes;

            String status = percent >= 100 ? "SLA_VENCIDO" : (percent >= 70 ? "PROXIMO_VENCIMENTO" : "NO_PRAZO");
            return new SlaCalculation(remaining, percent, status);
        } catch (Exception e) {
            return new SlaCalculation(null, null, "NO_PRAZO");
        }
    }

    private record SlaCalculation(Long remainingMinutes, Double percentConsumed, String status) {}
}

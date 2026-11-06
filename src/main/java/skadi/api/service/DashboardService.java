package skadi.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AlertRepository alertRepository;
    private final ColdRoomRepository coldRoomRepository;
    private final TemperatureReadingRepository readingRepository;
    private final DistributionCenterRepository cdRepository;
    private final DistributionCenterAccessService accessService;
    private final StringRedisTemplate redisTemplate;

    @Value("${skadi.redis.temperature-stream:skadi:leituras:temperatura}")
    private String temperatureStream;

    public record ChamberReading(BigDecimal temperature, LocalDateTime measuredAt) {}

    @Transactional(readOnly = true)
    public ManagerDashboardResponse getManagerDashboard(User user) {
        Long cdId = null;
        try {
            if (user != null) {
                List<Long> accessible = accessService.accessibleDistributionCenterIds(user);
                if (accessible != null && !accessible.isEmpty()) {
                    cdId = accessible.get(0);
                }
            }
        } catch (Exception ignored) {}
        if (cdId == null) {
            try {
                if (user != null && user.getCdId() != null) cdId = user.getCdId().longValue();
            } catch (Exception ignored) {}
        }
        if (cdId == null) cdId = 1L;
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

        // Puxa as leituras mais recentes prioritariamente do Redis
        Map<Long, ChamberReading> latestReadings = getLatestReadings(coldRooms, cdId);

        for (ColdRoom cr : coldRooms) {
            ChamberReading reading = latestReadings.get(cr.getId());
            if (reading == null || reading.temperature() == null) {
                semDadosCamaras++;
            } else if (reading.measuredAt() != null && Math.abs(Duration.between(reading.measuredAt(), now).toMinutes()) > 10) {
                offlineCamaras++;
            } else {
                double t = reading.temperature().doubleValue();
                double min = cr.getMinimumTemperature() != null ? cr.getMinimumTemperature().doubleValue() : -2.0;
                double max = cr.getMaximumTemperature() != null ? cr.getMaximumTemperature().doubleValue() : 4.0;
                if (t >= min && t <= max) {
                    naFaixaCamaras++;
                } else {
                    foraFaixaCamaras++;
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

            ChamberReading reading = cr != null ? latestReadings.get(cr.getId()) : null;
            BigDecimal currentTemp = reading != null ? reading.temperature() : null;

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

        // Leituras de tendência térmica da primeira câmara com prioridade ou #1 (do Redis ou fallback)
        Long targetRoomId = !priorityDtos.isEmpty() ? priorityDtos.get(0).coldRoomId() : (coldRooms.isEmpty() ? 1L : coldRooms.get(0).getId());
        ColdRoom targetCr = coldRooms.stream().filter(c -> c.getId().equals(targetRoomId)).findFirst().orElse(null);
        List<Float> trendReadings = getTrendReadings(targetRoomId, targetCr, cdId);

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

        // Puxa leituras atualizadas via Redis
        Map<Long, ChamberReading> latestReadings = getLatestReadings(coldRooms, cdId);

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

            ChamberReading reading = cr != null ? latestReadings.get(cr.getId()) : null;
            BigDecimal currentTemp = reading != null ? reading.temperature() : null;

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

    /**
     * Consulta as leituras de temperatura mais recentes a partir do Stream do Redis (skadi:leituras:temperatura).
     * Se o Redis estiver sem dados ou indisponível, executa fallback transparente para o PostgreSQL.
     */
    private Map<Long, ChamberReading> getLatestReadings(List<ColdRoom> coldRooms, Long cdId) {
        Map<Long, ChamberReading> readings = new HashMap<>();
        Set<String> targetThermometers = new HashSet<>();
        Map<String, Long> thermometerToChamberId = new HashMap<>();

        for (ColdRoom cr : coldRooms) {
            if (cr.getThermometer() != null && cr.getThermometer().getId() != null) {
                String termIdStr = cr.getThermometer().getId().toString();
                targetThermometers.add(termIdStr);
                thermometerToChamberId.put(termIdStr, cr.getId());
            }
        }

        // 1. Tenta buscar prioritariamente no Redis Stream
        if (redisTemplate != null && !targetThermometers.isEmpty()) {
            try {
                List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream()
                        .reverseRange(temperatureStream, Range.unbounded(), Limit.limit().count(1000));

                if (records != null) {
                    for (MapRecord<String, Object, Object> record : records) {
                        Map<Object, Object> value = record.getValue();
                        if (value == null) continue;

                        Object termObj = value.get("cod_termometro");
                        String termId = termObj != null ? termObj.toString() : null;

                        if (termId != null && targetThermometers.contains(termId)) {
                            Long chamberId = thermometerToChamberId.get(termId);
                            if (chamberId != null && !readings.containsKey(chamberId)) {
                                Object tempObj = value.get("temperatura");
                                Object timeObj = value.get("data_hora");

                                BigDecimal temp = null;
                                if (tempObj != null) {
                                    try {
                                        temp = new BigDecimal(tempObj.toString().trim());
                                    } catch (Exception ignored) {}
                                }

                                LocalDateTime measuredAt = null;
                                if (timeObj != null) {
                                    try {
                                        String timeStr = timeObj.toString().trim();
                                        if (timeStr.length() > 19) timeStr = timeStr.substring(0, 19);
                                        measuredAt = LocalDateTime.parse(timeStr);
                                    } catch (Exception ignored) {}
                                }

                                if (temp != null) {
                                    readings.put(chamberId, new ChamberReading(temp, measuredAt));
                                }
                            }
                        }
                        if (readings.size() == targetThermometers.size()) {
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Leitura no Redis indisponível, utilizando fallback para PostgreSQL: {}", e.getMessage());
            }
        }

        // 2. Para as câmaras que não estavam no Redis, complementa com PostgreSQL
        for (ColdRoom cr : coldRooms) {
            if (!readings.containsKey(cr.getId())) {
                List<Object[]> rows = readingRepository.findCurrentByColdRoomIdAndCd(cr.getId(), Math.toIntExact(cdId), true);
                if (!rows.isEmpty()) {
                    Object[] row = rows.get(0);
                    BigDecimal temp = row[4] instanceof BigDecimal ? (BigDecimal) row[4] : null;
                    LocalDateTime measuredAt = row[5] instanceof LocalDateTime ? (LocalDateTime) row[5] : null;
                    if (temp != null) {
                        readings.put(cr.getId(), new ChamberReading(temp, measuredAt));
                    }
                }
            }
        }

        return readings;
    }

    /**
     * Obtém série temporal das últimas leituras térmicas via Redis Stream, com fallback para PostgreSQL.
     */
    private List<Float> getTrendReadings(Long targetRoom, ColdRoom targetCr, Long cdId) {
        List<Float> trend = new ArrayList<>();

        if (redisTemplate != null && targetCr != null && targetCr.getThermometer() != null) {
            String termId = targetCr.getThermometer().getId().toString();
            try {
                List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream()
                        .reverseRange(temperatureStream, Range.unbounded(), Limit.limit().count(500));
                if (records != null) {
                    for (MapRecord<String, Object, Object> record : records) {
                        Map<Object, Object> value = record.getValue();
                        if (value == null) continue;
                        Object termObj = value.get("cod_termometro");
                        if (termObj != null && termId.equals(termObj.toString())) {
                            Object tempObj = value.get("temperatura");
                            if (tempObj != null) {
                                try {
                                    trend.add(Float.parseFloat(tempObj.toString().trim()));
                                } catch (Exception ignored) {}
                            }
                        }
                        if (trend.size() >= 24) break;
                    }
                }
            } catch (Exception ignored) {}
        }

        if (trend.isEmpty()) {
            List<Object[]> roomReadings = readingRepository.findCurrentByColdRoomIdAndCd(targetRoom, Math.toIntExact(cdId), true);
            for (Object[] r : roomReadings) {
                if (r[4] instanceof BigDecimal b) {
                    trend.add(b.floatValue());
                }
            }
        } else {
            Collections.reverse(trend);
        }

        return trend;
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

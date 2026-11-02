package skadi.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import skadi.api.dto.EmployeeDashboardResponse;
import skadi.api.dto.ManagerDashboardResponse;
import skadi.api.model.User;
import skadi.api.service.DashboardService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/dashboard")
@Tag(name = "Dashboard", description = "Endpoints consolidados para os dashboards do Gestor e Funcionário")
public class DashboardController {

    private final DashboardService service;

    @GetMapping("/manager")
    @Operation(summary = "Retorna os KPIs de intervenção, estado das câmaras e prioridades da unidade para o Gestor")
    public ResponseEntity<ManagerDashboardResponse> getManagerDashboard(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.getManagerDashboard(user));
    }

    @GetMapping("/employee")
    @Operation(summary = "Retorna o resumo da fila pessoal, alertas de SLA e próxima ação para o Funcionário/Operador")
    public ResponseEntity<EmployeeDashboardResponse> getEmployeeDashboard(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.getEmployeeDashboard(user));
    }
}

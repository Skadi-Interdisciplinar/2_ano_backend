package skadi.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import skadi.api.enums.AlertHandlingStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_atendimento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AlertHandling {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "cod_alerta", nullable = false)
    private Integer alertId;

    @Column(name = "cod_usuario")
    private Long userId;

    @Column(name = "data_hora_reconhecimento")
    private LocalDateTime acknowledgedAt;

    @Column(name = "data_hora_resolucao")
    private LocalDateTime resolvedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50, nullable = false)
    private AlertHandlingStatus status;
}

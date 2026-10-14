package skadi.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import skadi.api.enums.AlertStatus;
import skadi.api.enums.CurrentLevel;
import skadi.api.enums.SeverityLevel;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_alerta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Positive
    @Column(name = "cod_camara_frigorifica", nullable = false)
    private Integer coldRoomId;

    @Column(name = "vida_util_referencia_horas", precision = 7, scale = 2)
    private BigDecimal referenceShelfLifeHours;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_atual", length = 8, nullable = false)
    private CurrentLevel currentLevel;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "tipo", length = 100, nullable = false)
    private String type;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_gravidade", length = 100, nullable = false)
    private SeverityLevel severityLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 100, nullable = false)
    private AlertStatus status;
}

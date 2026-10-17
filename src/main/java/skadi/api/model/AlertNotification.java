package skadi.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_notificacao_alerta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AlertNotification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "cod_alerta", nullable = false)
    private Integer alertId;

    @Column(name = "cod_usuario", nullable = false)
    private Long userId;

    @Column(name = "data_hora_envio", insertable = false, updatable = false)
    private LocalDateTime sentAt;

    @Column(name = "data_hora_leitura")
    private LocalDateTime readAt;
}

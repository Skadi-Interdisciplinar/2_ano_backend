package skadi.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_leitura_temperatura")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TemperatureReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Positive
    @Column(name = "cod_termometro", nullable = false)
    private Integer thermometerId;

    @Column(name = "cod_alerta")
    private Integer alertId;

    @Column(name = "temperatura", nullable = false, precision = 5, scale = 2)
    private BigDecimal temperature;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime measuredAt;

    @Column(name = "id_evento_redis", unique = true, length = 100)
    private String eventId;
}

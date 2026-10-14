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
@Table(name = "tb_justificativa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Justification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "cod_atendimento", nullable = false)
    private Integer alertHandlingId;

    @Column(name = "motivo", length = 255, nullable = false)
    private String reason;

    @Column(name = "descricao", nullable = false)
    private String description;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime createdAt;
}

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
import skadi.api.enums.ReportStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_relatorio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "cod_usuario_gerador", nullable = false)
    private Integer generatedByUserId;

    @Column(name = "data_hora_geracao", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "hash_conteudo", length = 64, nullable = false)
    private String contentHash;

    @Column(name = "periodo_inicio", nullable = false)
    private LocalDate periodStart;

    @Column(name = "periodo_fim", nullable = false)
    private LocalDate periodEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50, nullable = false)
    private ReportStatus status;

    @Column(name = "conteudo", columnDefinition = "TEXT", nullable = false)
    private String content;
}

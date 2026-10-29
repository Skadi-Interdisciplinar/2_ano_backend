package skadi.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import skadi.api.enums.SupportCategory;
import skadi.api.enums.SupportStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_solicitacao_suporte")
@Getter
@Setter
@NoArgsConstructor
public class SupportRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cod_usuario_solicitante", nullable = false)
    private User solicitante;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", nullable = false, length = 20)
    private SupportCategory categoria;

    @Column(name = "descricao", nullable = false, columnDefinition = "text")
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SupportStatus status;

    @Column(name = "data_hora_abertura", nullable = false)
    private LocalDateTime dataHoraAbertura;

    @Column(name = "data_hora_encerramento")
    private LocalDateTime dataHoraEncerramento;

    @Column(name = "resposta", columnDefinition = "text")
    private String resposta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cod_usuario_responsavel")
    private User responsavel;
}

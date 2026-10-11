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

import java.time.LocalDate;

@Entity
@Table(name = "tb_assinatura")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Signature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "cod_relatorio", nullable = false)
    private Integer reportId;

    @Column(name = "certificado_titular", length = 255, nullable = false)
    private String certificateHolder;

    @Column(name = "numero_serie", length = 64, nullable = false)
    private String serialNumber;

    @Column(name = "autoridade_certificadora", length = 255, nullable = false)
    private String certifyingAuthority;

    @Column(name = "algoritmo_assinatura", length = 50, nullable = false)
    private String signatureAlgorithm;

    @Column(name = "certificado_publico", nullable = false, columnDefinition = "TEXT")
    private String certificatePublicKey;

    @Column(name = "assinatura", nullable = false, columnDefinition = "TEXT")
    private String signature;

    @Column(name = "data_assinatura", nullable = false)
    private LocalDate signedAt;

    @Column(name = "carimbo_tempo", nullable = false, columnDefinition = "TEXT")
    private String timestampToken;
}

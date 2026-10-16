package skadi.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tb_camara_frigorifica")
public class ColdRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "modelo", nullable = false, length = 150)
    private String model;

    @Column(name = "localizacao", nullable = false, length = 100)
    private String location;

    @Column(name = "temperatura_min", nullable = false, precision = 5, scale = 2)
    private BigDecimal minimumTemperature;

    @Column(name = "temperatura_max", nullable = false, precision = 5, scale = 2)
    private BigDecimal maximumTemperature;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cod_cd", nullable = false)
    private DistributionCenter distributionCenter;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cod_termometro", nullable = false, unique = true)
    private Thermometer thermometer;
}

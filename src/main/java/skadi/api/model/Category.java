package skadi.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Duration;

@Data
@NoArgsConstructor
@Entity
@Table(name = "tb_categoria")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, unique = true, length = 150)
    private String name;

    @Column(name = "temperatura_min", nullable = false, precision = 5, scale = 2)
    private BigDecimal minimumTemperature;

    @Column(name = "temperatura_max", nullable = false, precision = 5, scale = 2)
    private BigDecimal maximumTemperature;

    @JdbcTypeCode(SqlTypes.INTERVAL_SECOND)
    @Column(name = "vida_util_horas", nullable = false, columnDefinition = "interval hour to minute")
    private Duration shelfLife;

    public Category(Long id, String name, BigDecimal minimumTemperature,
                    BigDecimal maximumTemperature, Duration shelfLife) {
        this.id = id;
        this.name = name;
        this.minimumTemperature = minimumTemperature;
        this.maximumTemperature = maximumTemperature;
        this.shelfLife = shelfLife;
    }
}

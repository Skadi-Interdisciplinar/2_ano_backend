package skadi.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnTransformer;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tb_categoria")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, unique = true, length = 150)
    private String name;

    @Column(name = "temperatura_ideal", nullable = false, precision = 5, scale = 2)
    private BigDecimal idealTemperature;

    @Column(name = "vida_util_horas", nullable = false, columnDefinition = "INTERVAL HOUR TO MINUTE")
    @ColumnTransformer(
            read = "ROUND(EXTRACT(EPOCH FROM vida_util_horas) / 3600.0, 2)",
            write = "(ROUND(?::numeric * 60)::double precision * INTERVAL '1 minute')"
    )
    private BigDecimal shelfLifeHours;
}

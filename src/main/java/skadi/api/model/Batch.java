package skadi.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import skadi.api.converter.BatchStatusConverter;
import skadi.api.enums.BatchStatus;

import java.time.LocalDate;

@Entity
@Table(name = "tb_lote")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Batch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_lote", nullable = false, unique = true, length = 50)
    private String batchCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cod_categoria", nullable = false)
    private Category category;

    @Column(name = "data_fabricacao", nullable = false)
    private LocalDate manufacturingDate;

    @Column(name = "data_validade", nullable = false)
    private LocalDate expirationDate;

    @Convert(converter = BatchStatusConverter.class)
    @Column(name = "status", nullable = false, length = 20)
    private BatchStatus status;
}

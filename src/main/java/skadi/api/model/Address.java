package skadi.api.model;

import jakarta.persistence.Column;
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

@Entity
@Table(name = "tb_endereco")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cep", nullable = false, unique = true, length = 8)
    private String zipCode;

    @Column(name = "rua", nullable = false, length = 150)
    private String street;

    @Column(name = "numero", nullable = false)
    private Integer number;

    @Column(name = "cidade", nullable = false, length = 60)
    private String city;

    @Column(name = "bairro", nullable = false, length = 70)
    private String neighborhood;

    @Column(name = "complemento", length = 50)
    private String complement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cod_estado", nullable = false)
    private State state;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cod_cd", nullable = false)
    private DistributionCenter distributionCenter;
}

package skadi.api.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

import javax.management.MXBean;

@Entity
@Table(name = "tb_cd")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DistributionCenter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(max = 150,message = "Nome não pode passar de 150 carcteres")
    @Column(name = "nome")
    private String name;

    @Size(min = 14,max = 14, message = "CNPJ precisa de 14 carcteres")
    private String cnpj;

    public DistributionCenter(String name, String cnpj) {
        this.name = name;
        this.cnpj = cnpj;
    }


}

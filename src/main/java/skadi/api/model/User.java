package skadi.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Check;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import skadi.api.enums.NivelAcesso;
import skadi.api.exceptions.GestorAssociationRuleException;

import java.util.Collection;
import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tb_usuario")
@Check(constraints = "cod_gestor IS NULL OR cod_gestor <> id")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;

    @Column(unique = true)
    private String username;
    @Size(max = 11,min = 11,message = "Cpf precisa ter 11 caracteres")
    private String cpf;

    @Column(unique = true)
    private String email;


    private String senha;
    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_acesso")
    private NivelAcesso nivelAcesso;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "cod_cd" )
    private DistributionCenter cdGestor;

     @ManyToMany
     @JoinTable(name = "tb_admin_cd",
             joinColumns = @JoinColumn(name = "cod_admin"),
             inverseJoinColumns = @JoinColumn(name = "cod_cd")
     )
     private List<DistributionCenter> cd;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cod_gestor")
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User gestor;

    public User(String nome, String username, String cpf, String email, String senha, NivelAcesso nivelAcesso, DistributionCenter cdGestor, User gestor) {
        this.nome = nome;
        this.username = username;
        this.cpf = cpf;
        this.email = email;
        this.senha = senha;
        this.nivelAcesso = nivelAcesso;
        this.cdGestor = cdGestor;
        this.cd = null;
        this.gestor = gestor;
    }

    public User(String nome, String username, String cpf, String email, String senha, NivelAcesso nivelAcesso, List<DistributionCenter> cd, User gestor) {
        this.nome = nome;
        this.username = username;
        this.cpf = cpf;
        this.email = email;
        this.senha = senha;
        this.nivelAcesso = nivelAcesso;
        this.cdGestor = null;
        this.cd = cd;
        this.gestor = gestor;
    }

    @PrePersist
    @PreUpdate
    public void validateGestorAssociation() {
        if (gestor == null || gestor.getId() == null) {
            return;
        }
        if (id != null && id.equals(gestor.getId())) {
            throw new GestorAssociationRuleException("O usuário não pode ser o próprio gestor.");
        }
        if (cdGestor == null || cdGestor.getId() == null) {
            return;
        }
        DistributionCenter gestorCd = gestor.getCdGestor();
        if (gestorCd == null || gestorCd.getId() == null || !cdGestor.getId().equals(gestorCd.getId())) {
            throw new GestorAssociationRuleException(
                    "O gestor responsável deve pertencer ao mesmo centro de distribuição.");
        }
    }

    public Integer getCdId() {
        if (cdGestor != null && cdGestor.getId() != null) {
            return Math.toIntExact(cdGestor.getId());
        }
        if (cd != null && !cd.isEmpty() && cd.get(0).getId() != null) {
            return Math.toIntExact(cd.get(0).getId());
        }
        return null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (nivelAcesso == null) {
            return List.of();
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + nivelAcesso.name()));
    }

    @Override
    public @Nullable String getPassword() {
        return this.senha;
    }
}

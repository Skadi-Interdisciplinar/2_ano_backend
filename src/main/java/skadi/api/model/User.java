package skadi.api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import skadi.api.enums.NivelAcesso;

import java.util.Collection;
import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tb_usuario")

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

    @ManyToOne
    @JoinColumn(name = "cod_cd")
    private DistributionCenter cdGestor;

     @ManyToMany
     @JoinTable(name = "tb_admin_cd",
             joinColumns = @JoinColumn(name = "cod_admin"),
             inverseJoinColumns = @JoinColumn(name = "cod_cd")
     )
     private List<DistributionCenter> cd;

     @Column(name = "cod_gestor")
    private Integer codGestor;

    public User(String nome, String username, String cpf, String email, String senha, NivelAcesso nivelAcesso, DistributionCenter cdGestor, Integer codGestor) {
        this.nome = nome;
        this.username = username;
        this.cpf = cpf;
        this.email = email;
        this.senha = senha;
        this.nivelAcesso = nivelAcesso;
        this.cdGestor = cdGestor;
        this.cd = null;
        this.codGestor = codGestor;
    }

    public User(String nome, String username, String cpf, String email, String senha, NivelAcesso nivelAcesso, List<DistributionCenter> cd, Integer codGestor) {
        this.nome = nome;
        this.username = username;
        this.cpf = cpf;
        this.email = email;
        this.senha = senha;
        this.nivelAcesso = nivelAcesso;
        this.cdGestor = null;
        this.cd = cd;
        this.codGestor = codGestor;
    }

    public Integer getCdId() {
        if (cdGestor != null && cdGestor.getId() != null) {
            return Math.toIntExact(cdGestor.getId());
        }
        if (cd != null && !cd.isEmpty() && cd.getFirst().getId() != null) {
            return Math.toIntExact(cd.getFirst().getId());
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

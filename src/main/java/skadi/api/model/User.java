package skadi.api.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import skadi.api.enums.NivelAcesso;
import org.hibernate.validator.constraints.Length;

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
    private String username;
    @Length(max = 11)
    private String cpf;
    private String email;
    private String senha;
    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_acesso")
    private NivelAcesso nivelAcesso;
    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "id")
    // TODO: adicionar CD
    @Column(name = "cod_cd")
    private Integer codCD;

    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "id")
    // TODO: adicionar gestor
    @Column(name = "cod_gestor")
    private Integer codGestor;



    public User(String nome, String username, String cpf, String email, String senha, NivelAcesso nivelAcesso, Integer codCD, Integer codGestor) {
        this.nome = nome;
        this.username = username;
        this.cpf = cpf;
        this.email = email;
        this.senha = senha;
        this.nivelAcesso = nivelAcesso;
        this.codCD = codCD;
        this.codGestor = codGestor;
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

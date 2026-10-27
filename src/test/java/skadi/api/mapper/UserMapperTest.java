package skadi.api.mapper;

import org.junit.jupiter.api.Test;
import skadi.api.dto.RegisterRequest;
import skadi.api.enums.NivelAcesso;
import skadi.api.model.DistributionCenter;
import skadi.api.model.User;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserMapperTest {

    @Test
    void fromDTO_mapsAdminWithCdListAndNullUniqueCd() {
        DistributionCenter osasco = new DistributionCenter(7L, "CD 7", "12345678901234", null);
        RegisterRequest request = new RegisterRequest(
                "Enzo", "enzo", "12345678901", "enzo@email.com", "senha", NivelAcesso.ADMIN,
                osasco, List.of(osasco), 10
        );

        User user = UserMapper.fromDTO(request);

        assertNull(user.getId());
        assertEquals("Enzo", user.getNome());
        assertEquals("enzo", user.getUsername());
        assertEquals("12345678901", user.getCpf());
        assertEquals("enzo@email.com", user.getEmail());
        assertEquals("senha", user.getSenha());
        assertEquals(NivelAcesso.ADMIN, user.getNivelAcesso());
        assertNull(user.getCdGestor());
        assertEquals(List.of(osasco), user.getCd());
        assertEquals(10, user.getCodGestor());
    }

    @Test
    void fromDTO_mapsGestorWithUniqueCdAndNullList() {
        DistributionCenter osasco = new DistributionCenter(7L, "CD 7", "12345678901234", null);
        RegisterRequest request = new RegisterRequest(
                "Enzo", "enzo", "12345678901", "enzo@email.com", "senha", NivelAcesso.GESTOR,
                osasco, List.of(osasco), 2
        );

        User user = UserMapper.fromDTO(request);

        assertEquals(NivelAcesso.GESTOR, user.getNivelAcesso());
        assertEquals(osasco, user.getCdGestor());
        assertNull(user.getCd());
        assertEquals(2, user.getCodGestor());
    }

    @Test
    void fromDTO_mapsOperadorWithUniqueCdAndNullList() {
        DistributionCenter osasco = new DistributionCenter(7L, "CD 7", "12345678901234", null);
        RegisterRequest request = new RegisterRequest(
                "Enzo", "enzo", "12345678901", "enzo@email.com", "senha", NivelAcesso.OPERADOR,
                osasco, List.of(osasco), 2
        );

        User user = UserMapper.fromDTO(request);

        assertEquals(NivelAcesso.OPERADOR, user.getNivelAcesso());
        assertEquals(osasco, user.getCdGestor());
        assertNull(user.getCd());
        assertEquals(2, user.getCodGestor());
    }

    @Test
    void fromDTO_mapsSuperAdminWithBothCdAssociationsNull() {
        DistributionCenter osasco = new DistributionCenter(7L, "CD 7", "12345678901234", null);
        RegisterRequest request = new RegisterRequest(
                "Root", "root", "12345678901", "root@email.com", "senha", NivelAcesso.SUPER_ADMIN,
                osasco, List.of(osasco), null
        );

        User user = UserMapper.fromDTO(request);

        assertEquals(NivelAcesso.SUPER_ADMIN, user.getNivelAcesso());
        assertNull(user.getCdGestor());
        assertNull(user.getCd());
        assertNull(user.getCodGestor());
    }

    @Test
    void toDTO_copiesAdminFieldsWithCdList() {
        DistributionCenter osasco = new DistributionCenter(7L, "CD 7", "12345678901234", null);
        User user = new User(3L, "Enzo", "enzo", "12345678901", "enzo@email.com", "senha",
                NivelAcesso.ADMIN, null, List.of(osasco), 10);

        var dto = UserMapper.toDTO(user);

        assertEquals(3L, dto.id());
        assertEquals("enzo", dto.username());
        assertNull(dto.cdGestor());
        assertEquals(List.of(osasco), dto.cd());
        assertEquals(10, dto.codGestor());
    }

    @Test
    void toDTO_copiesOperadorFieldsWithUniqueCd() {
        DistributionCenter osasco = new DistributionCenter(7L, "CD 7", "12345678901234", null);
        User user = new User(3L, "Enzo", "enzo", "12345678901", "enzo@email.com", "senha",
                NivelAcesso.OPERADOR, osasco, null, 2);

        var dto = UserMapper.toDTO(user);

        assertEquals(3L, dto.id());
        assertEquals("enzo", dto.username());
        assertEquals(osasco, dto.cdGestor());
        assertNull(dto.cd());
        assertEquals(2, dto.codGestor());
    }
}

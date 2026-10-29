package skadi.api.mapper;

import org.junit.jupiter.api.Test;
import skadi.api.dto.DistributionCenterResponse;
import skadi.api.dto.RegisterRequest;
import skadi.api.dto.UserRequest;
import skadi.api.dto.UserResponse;
import skadi.api.enums.NivelAcesso;
import skadi.api.model.DistributionCenter;
import skadi.api.model.User;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

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

    @Test
    void toResponse_mapsAdminWithoutPasswordAndWithCdList() {
        DistributionCenter osasco = new DistributionCenter(7L, "CD 7", "12345678901234", null);
        User user = new User(3L, "Enzo", "enzo", "12345678901", "enzo@email.com", "senha",
                NivelAcesso.ADMIN, null, List.of(osasco), null);

        UserResponse response = UserMapper.toResponse(user);

        assertEquals(3L, response.id());
        assertEquals("Enzo", response.nome());
        assertEquals("enzo", response.username());
        assertEquals("12345678901", response.cpf());
        assertEquals("enzo@email.com", response.email());
        assertEquals(NivelAcesso.ADMIN, response.nivelAcesso());
        assertNull(response.cdGestor());
        assertEquals(7L, response.cd().getFirst().id());
        assertEquals("CD 7", response.cd().getFirst().name());
        assertNull(response.codGestor());
    }

    @Test
    void toResponse_mapsOperadorWithUniqueCd() {
        DistributionCenter osasco = new DistributionCenter(7L, "CD 7", "12345678901234", null);
        User user = new User(4L, "Ana", "ana", "10987654321", "ana@email.com", "senha",
                NivelAcesso.OPERADOR, osasco, null, 2);

        UserResponse response = UserMapper.toResponse(user);

        assertEquals(4L, response.id());
        assertEquals(NivelAcesso.OPERADOR, response.nivelAcesso());
        assertEquals(7L, response.cdGestor().id());
        assertNull(response.cd());
        assertEquals(2, response.codGestor());
    }

    @Test
    void update_appliesAdminFieldsAndKeepsIdAndPassword() {
        DistributionCenter antigo = new DistributionCenter(1L, "CD antigo", "11111111111111", null);
        User user = new User(3L, "Enzo", "enzo", "12345678901", "enzo@email.com", "senha-atual",
                NivelAcesso.OPERADOR, antigo, null, 2);
        UserRequest request = new UserRequest(
                99L, "Enzo Nascimento", "enzo.n", "10987654321", "novo@email.com", NivelAcesso.ADMIN,
                new DistributionCenterResponse(1L, "CD antigo", "11111111111111"),
                List.of(new DistributionCenterResponse(7L, "CD 7", "12345678901234")),
                null
        );

        User updated = UserMapper.update(user, request);

        assertSame(user, updated);
        assertEquals(3L, updated.getId());
        assertEquals("senha-atual", updated.getSenha());
        assertEquals("Enzo Nascimento", updated.getNome());
        assertEquals("enzo.n", updated.getUsername());
        assertEquals("10987654321", updated.getCpf());
        assertEquals("novo@email.com", updated.getEmail());
        assertEquals(NivelAcesso.ADMIN, updated.getNivelAcesso());
        assertSame(antigo, updated.getCdGestor());
        assertNull(updated.getCd());
        assertNull(updated.getCodGestor());
    }

    @Test
    void update_keepsExistingDistributionCenters() {
        DistributionCenter osasco = new DistributionCenter(7L, "CD 7", "12345678901234", null);
        User user = new User(4L, "Ana", "ana", "10987654321", "ana@email.com", "senha",
                NivelAcesso.ADMIN, null, List.of(osasco), null);
        UserRequest request = new UserRequest(
                4L, "Ana", "ana", "10987654321", "ana@email.com", NivelAcesso.OPERADOR,
                new DistributionCenterResponse(8L, "CD 8", "22345678901234"),
                List.of(new DistributionCenterResponse(7L, "CD 7", "12345678901234")),
                2
        );

        User updated = UserMapper.update(user, request);

        assertEquals(NivelAcesso.OPERADOR, updated.getNivelAcesso());
        assertNull(updated.getCdGestor());
        assertEquals(List.of(osasco), updated.getCd());
        assertEquals(2, updated.getCodGestor());
        assertEquals("senha", updated.getSenha());
    }
}

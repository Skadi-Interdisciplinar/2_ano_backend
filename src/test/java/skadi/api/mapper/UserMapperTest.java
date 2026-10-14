package skadi.api.mapper;

import org.junit.jupiter.api.Test;
import skadi.api.dto.RegisterRequest;
import skadi.api.enums.NivelAcesso;
import skadi.api.model.DistributionCenter;
import skadi.api.model.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserMapperTest {

    @Test
    void fromDTO_copiesRegistrationFields() {
        DistributionCenter cd = new DistributionCenter(7L, "CD 7", "12345678901234");
        RegisterRequest request = new RegisterRequest(
                "Enzo", "enzo", "12345678901", "enzo@email.com", "senha", NivelAcesso.GESTOR, cd, 2
        );

        User user = UserMapper.fromDTO(request);

        assertNull(user.getId());
        assertEquals("Enzo", user.getNome());
        assertEquals("enzo", user.getUsername());
        assertEquals("12345678901", user.getCpf());
        assertEquals("enzo@email.com", user.getEmail());
        assertEquals("senha", user.getSenha());
        assertEquals(NivelAcesso.GESTOR, user.getNivelAcesso());
        assertEquals(cd, user.getCd());
        assertEquals(2, user.getCodGestor());
    }

    @Test
    void toDTO_copiesUserFieldsIncludingCodGestor() {
        DistributionCenter cd = new DistributionCenter(7L, "CD 7", "12345678901234");
        User user = new User(3L, "Enzo", "enzo", "12345678901", "enzo@email.com", "senha", NivelAcesso.OPERARIO, cd, 2);

        var dto = UserMapper.toDTO(user);

        assertEquals(3L, dto.id());
        assertEquals("enzo", dto.username());
        assertEquals(cd, dto.cd());
        assertEquals(2, dto.codGestor());
    }
}

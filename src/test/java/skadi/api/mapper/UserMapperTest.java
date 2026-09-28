package skadi.api.mapper;

import org.junit.jupiter.api.Test;
import skadi.api.dto.RegisterRequest;
import skadi.api.enums.NivelAcesso;
import skadi.api.model.User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserMapperTest {

    @Test
    void fromDTO_copiesRegistrationFields() {
        RegisterRequest request = new RegisterRequest(
                "Enzo", "enzo", "12345678901", "enzo@email.com", "senha", NivelAcesso.GESTOR, 7
        );

        User user = UserMapper.fromDTO(request);

        assertNull(user.getId());
        assertEquals("Enzo", user.getNome());
        assertEquals("enzo", user.getUsername());
        assertEquals("12345678901", user.getCpf());
        assertEquals("enzo@email.com", user.getEmail());
        assertEquals("senha", user.getSenha());
        assertEquals(NivelAcesso.GESTOR, user.getNivelAcesso());
        assertEquals(7, user.getCodCD());
    }
}

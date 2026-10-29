package skadi.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import skadi.api.dto.DistributionCenterResponse;
import skadi.api.dto.UserRequest;
import skadi.api.dto.UserResponse;
import skadi.api.enums.NivelAcesso;
import skadi.api.exceptions.AccessLevelChangeNotAllowedException;
import skadi.api.exceptions.UserNotFoundException;
import skadi.api.model.DistributionCenter;
import skadi.api.model.User;
import skadi.api.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuperAdminServiceTest {

    @Mock
    private UserRepository repository;

    private SuperAdminService service;

    @BeforeEach
    void setUp() {
        service = new SuperAdminService(repository);
    }

    @Test
    void updateChangesEditableFieldsAndKeepsAccessLevel() {
        DistributionCenter osasco = new DistributionCenter(7L, "CD 7", "12345678901234", null);
        User user = admin(osasco);
        when(repository.findById(3L)).thenReturn(Optional.of(user));
        when(repository.save(user)).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = service.update(3L, request(NivelAcesso.ADMIN));

        assertEquals(3L, response.id());
        assertEquals("Enzo Nascimento", response.nome());
        assertEquals("enzo.n", response.username());
        assertEquals("10987654321", response.cpf());
        assertEquals("novo@email.com", response.email());
        assertEquals(NivelAcesso.ADMIN, response.nivelAcesso());
        assertEquals(10, response.codGestor());
        assertNull(response.cdGestor());
        assertEquals(7L, response.cd().getFirst().id());
        assertEquals("senha-atual", user.getSenha());
        verify(repository).save(user);
    }

    @Test
    void updateRejectsAccessLevelChange() {
        User user = admin(null);
        when(repository.findById(3L)).thenReturn(Optional.of(user));

        assertThrows(AccessLevelChangeNotAllowedException.class,
                () -> service.update(3L, request(NivelAcesso.GESTOR)));

        assertEquals(NivelAcesso.ADMIN, user.getNivelAcesso());
        assertEquals("Enzo", user.getNome());
        verify(repository, never()).save(any());
    }

    @Test
    void updateRejectsMissingAccessLevel() {
        User user = admin(null);
        when(repository.findById(3L)).thenReturn(Optional.of(user));

        assertThrows(AccessLevelChangeNotAllowedException.class,
                () -> service.update(3L, request(null)));

        verify(repository, never()).save(any());
    }

    @Test
    void updateRejectsUnknownUser() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> service.update(99L, request(NivelAcesso.ADMIN)));

        assertEquals("Usuário não encontrado", exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void deleteRemovesExistingUser() {
        when(repository.existsById(3L)).thenReturn(true);

        service.delete(3L);

        verify(repository).deleteById(3L);
    }

    @Test
    void deleteRejectsUnknownUser() {
        when(repository.existsById(99L)).thenReturn(false);

        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> service.delete(99L));

        assertEquals("Usuário não encontrado", exception.getMessage());
        verify(repository, never()).deleteById(any());
    }

    private User admin(DistributionCenter distributionCenter) {
        List<DistributionCenter> centers = distributionCenter == null ? null : List.of(distributionCenter);
        return new User(3L, "Enzo", "enzo", "12345678901", "enzo@email.com", "senha-atual",
                NivelAcesso.ADMIN, null, centers, null);
    }

    private UserRequest request(NivelAcesso nivelAcesso) {
        return new UserRequest(
                3L,
                "Enzo Nascimento",
                "enzo.n",
                "10987654321",
                "novo@email.com",
                nivelAcesso,
                null,
                List.of(new DistributionCenterResponse(7L, "CD 7", "12345678901234")),
                10
        );
    }
}

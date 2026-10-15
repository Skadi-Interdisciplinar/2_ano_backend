package skadi.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import skadi.api.dto.StateResponse;
import skadi.api.exceptions.StateNotFoundException;
import skadi.api.model.State;
import skadi.api.repository.StateRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StateServiceTest {

    @Mock
    private StateRepository repository;

    private StateService service;

    @BeforeEach
    void setUp() {
        service = new StateService(repository);
    }

    @Test
    void findAllReturnsStates() {
        when(repository.findAll(any(Sort.class))).thenReturn(List.of(
                new State(1L, "AC"),
                new State(25L, "SP")
        ));

        List<StateResponse> response = service.findAll();

        assertEquals(2, response.size());
        assertEquals("AC", response.getFirst().abbreviation());
    }

    @Test
    void findByIdReturnsState() {
        when(repository.findById(25L)).thenReturn(Optional.of(new State(25L, "SP")));

        StateResponse response = service.findById(25L);

        assertEquals("SP", response.abbreviation());
    }

    @Test
    void findByIdRejectsUnknownState() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(StateNotFoundException.class, () -> service.findById(99L));
    }
}

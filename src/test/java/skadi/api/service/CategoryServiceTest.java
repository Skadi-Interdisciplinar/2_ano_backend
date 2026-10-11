package skadi.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import skadi.api.dto.CategoryRequest;
import skadi.api.dto.CategoryResponse;
import skadi.api.exceptions.CategoryAlreadyExistsException;
import skadi.api.exceptions.CategoryNotFoundException;
import skadi.api.model.Category;
import skadi.api.repository.CategoryRepository;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository repository;

    private CategoryService service;

    @BeforeEach
    void setUp() {
        service = new CategoryService(repository);
    }

    @Test
    void createSavesCategoryWhenNameIsAvailable() {
        CategoryRequest request = request("  Congelados  ");
        when(repository.existsByNomeIgnoreCase("Congelados")).thenReturn(false);
        when(repository.save(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            category.setId(1L);
            return category;
        });

        CategoryResponse response = service.create(request);

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(repository).save(captor.capture());
        assertEquals(1L, response.id());
        assertEquals("Congelados", response.nome());
        assertEquals("Congelados", captor.getValue().getNome());
    }

    @Test
    void createRejectsDuplicatedName() {
        CategoryRequest request = request("Congelados");
        when(repository.existsByNomeIgnoreCase("Congelados")).thenReturn(true);

        assertThrows(CategoryAlreadyExistsException.class, () -> service.create(request));
    }

    @Test
    void findByIdReturnsCategory() {
        Category category = category();
        when(repository.findById(1L)).thenReturn(Optional.of(category));

        CategoryResponse response = service.findById(1L);

        assertEquals(category.getId(), response.id());
        assertEquals(category.getTemperaturaIdeal(), response.temperaturaIdeal());
    }

    @Test
    void findByIdRejectsUnknownId() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class, () -> service.findById(99L));
    }

    @Test
    void updateChangesCategoryValues() {
        Category category = category();
        CategoryRequest request = new CategoryRequest(
                "Resfriados",
                new BigDecimal("4.00"),
                new BigDecimal("72.00")
        );
        when(repository.findById(1L)).thenReturn(Optional.of(category));
        when(repository.existsByNomeIgnoreCaseAndIdNot("Resfriados", 1L)).thenReturn(false);
        when(repository.save(category)).thenReturn(category);

        CategoryResponse response = service.update(1L, request);

        assertEquals("Resfriados", response.nome());
        assertEquals(new BigDecimal("4.00"), response.temperaturaIdeal());
        assertEquals(new BigDecimal("72.00"), response.vidaUtilHoras());
    }

    @Test
    void deleteRemovesExistingCategory() {
        Category category = category();
        when(repository.findById(1L)).thenReturn(Optional.of(category));

        service.delete(1L);

        verify(repository).delete(category);
        verify(repository).flush();
    }

    private CategoryRequest request(String nome) {
        return new CategoryRequest(
                nome,
                new BigDecimal("-18.00"),
                new BigDecimal("48.00")
        );
    }

    private Category category() {
        return new Category(
                1L,
                "Congelados",
                new BigDecimal("-18.00"),
                new BigDecimal("48.00")
        );
    }
}

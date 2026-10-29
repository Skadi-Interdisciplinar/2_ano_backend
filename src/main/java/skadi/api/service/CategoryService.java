package skadi.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skadi.api.dto.CategoryRequest;
import skadi.api.dto.CategoryResponse;
import skadi.api.exceptions.CategoryAlreadyExistsException;
import skadi.api.exceptions.CategoryInvalidTemperatureRangeException;
import skadi.api.exceptions.CategoryNotFoundException;
import skadi.api.mapper.CategoryMapper;
import skadi.api.model.Category;
import skadi.api.model.User;
import skadi.api.enums.NivelAcesso;
import skadi.api.repository.CategoryRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository repository;
    private final DistributionCenterAccessService accessService;
    private final AuditUserContext auditUserContext;

    @Transactional
    public CategoryResponse create(CategoryRequest request, User user) {
        accessService.requireRole(user, NivelAcesso.ADMIN,
                "Somente ADMIN pode cadastrar categorias.");
        validateTemperatureRange(request);
        String name = request.name().trim();
        if (repository.existsByNameIgnoreCase(name)) {
            throw new CategoryAlreadyExistsException("Já existe uma categoria com esse nome");
        }

        auditUserContext.setCurrentUser(user.getId());
        Category category = CategoryMapper.fromRequest(request);
        return CategoryMapper.toResponse(repository.save(category));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "name"))
                .stream()
                .map(CategoryMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse findById(Long id) {
        return CategoryMapper.toResponse(findEntityById(id));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request, User user) {
        accessService.requireRole(user, NivelAcesso.ADMIN,
                "Somente ADMIN pode alterar categorias.");
        Category category = findEntityById(id);
        validateTemperatureRange(request);
        String name = request.name().trim();
        if (repository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new CategoryAlreadyExistsException("Já existe outra categoria com esse nome");
        }

        auditUserContext.setCurrentUser(user.getId());
        CategoryMapper.update(category, request);
        return CategoryMapper.toResponse(repository.save(category));
    }

    @Transactional
    public void delete(Long id, User user) {
        accessService.requireRole(user, NivelAcesso.ADMIN,
                "Somente ADMIN pode excluir categorias.");
        Category category = findEntityById(id);
        auditUserContext.setCurrentUser(user.getId());
        repository.delete(category);
        repository.flush();
    }

    private Category findEntityById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(
                        "Categoria não encontrada com o id " + id));
    }

    private void validateTemperatureRange(CategoryRequest request) {
        if (request.minimumTemperature().compareTo(request.maximumTemperature()) >= 0) {
            throw new CategoryInvalidTemperatureRangeException();
        }
    }
}

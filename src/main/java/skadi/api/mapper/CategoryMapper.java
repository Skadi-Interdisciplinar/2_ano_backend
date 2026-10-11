package skadi.api.mapper;

import skadi.api.dto.CategoryRequest;
import skadi.api.dto.CategoryResponse;
import skadi.api.model.Category;

public final class CategoryMapper {

    private CategoryMapper() {
    }

    public static Category fromRequest(CategoryRequest request) {
        return new Category(
                null,
                request.nome().trim(),
                request.temperaturaIdeal(),
                request.vidaUtilHoras()
        );
    }

    public static void update(Category category, CategoryRequest request) {
        category.setNome(request.nome().trim());
        category.setTemperaturaIdeal(request.temperaturaIdeal());
        category.setVidaUtilHoras(request.vidaUtilHoras());
    }

    public static CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getNome(),
                category.getTemperaturaIdeal(),
                category.getVidaUtilHoras()
        );
    }
}

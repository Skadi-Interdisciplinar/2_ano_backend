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
                request.name().trim(),
                request.idealTemperature(),
                request.shelfLifeHours()
        );
    }

    public static void update(Category category, CategoryRequest request) {
        category.setName(request.name().trim());
        category.setIdealTemperature(request.idealTemperature());
        category.setShelfLifeHours(request.shelfLifeHours());
    }

    public static CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getIdealTemperature(),
                category.getShelfLifeHours()
        );
    }
}

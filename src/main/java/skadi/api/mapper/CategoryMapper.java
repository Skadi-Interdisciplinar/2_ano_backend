package skadi.api.mapper;

import skadi.api.dto.CategoryRequest;
import skadi.api.dto.CategoryResponse;
import skadi.api.model.Category;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

public final class CategoryMapper {

    private CategoryMapper() {
    }

    public static Category fromRequest(CategoryRequest request) {
        return new Category(
                null,
                request.name().trim(),
                request.minimumTemperature(),
                request.maximumTemperature(),
                toDuration(request.shelfLifeHours())
        );
    }

    public static void update(Category category, CategoryRequest request) {
        category.setName(request.name().trim());
        category.setMinimumTemperature(request.minimumTemperature());
        category.setMaximumTemperature(request.maximumTemperature());
        category.setShelfLife(toDuration(request.shelfLifeHours()));
    }

    public static CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getMinimumTemperature(),
                category.getMaximumTemperature(),
                toHours(category.getShelfLife())
        );
    }

    private static Duration toDuration(BigDecimal hours) {
        long seconds = hours.multiply(BigDecimal.valueOf(3600))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
        return Duration.ofSeconds(seconds);
    }

    private static BigDecimal toHours(Duration duration) {
        return BigDecimal.valueOf(duration.toSeconds())
                .divide(BigDecimal.valueOf(3600), 2, RoundingMode.HALF_UP);
    }
}

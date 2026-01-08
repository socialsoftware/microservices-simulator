package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.aggregate;

import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.CategoryDto;

public interface CategoryFactory {
    Category createCategory(Integer aggregateId, CategoryDto categoryDto);
    Category createCategoryFromExisting(Category existingCategory);
    CategoryDto createCategoryDto(Category category);
}

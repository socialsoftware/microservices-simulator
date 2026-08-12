package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.aggregate.sagas.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.aggregate.Category;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.CategoryDto;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.aggregate.CategoryFactory;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.aggregate.sagas.SagaCategory;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.aggregate.sagas.dtos.SagaCategoryDto;

@Service
@Profile("sagas")
public class SagasCategoryFactory implements CategoryFactory {
    @Override
    public Category createCategory(Integer aggregateId, CategoryDto categoryDto) {
        return new SagaCategory(aggregateId, categoryDto);
    }

    @Override
    public Category createCategoryFromExisting(Category existingCategory) {
        return new SagaCategory((SagaCategory) existingCategory);
    }

    @Override
    public CategoryDto createCategoryDto(Category category) {
        return new SagaCategoryDto(category);
    }
}
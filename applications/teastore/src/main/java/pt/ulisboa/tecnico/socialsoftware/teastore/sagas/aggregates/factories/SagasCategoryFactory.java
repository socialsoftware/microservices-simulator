package pt.ulisboa.tecnico.socialsoftware.teastore.sagas.aggregates.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.aggregate.Category;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.CategoryDto;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.aggregate.CategoryFactory;
import pt.ulisboa.tecnico.socialsoftware.teastore.sagas.aggregates.SagaCategory;
import pt.ulisboa.tecnico.socialsoftware.teastore.sagas.aggregates.dtos.SagaCategoryDto;

@Service
@Profile("sagas")
public class SagasCategoryFactory extends CategoryFactory {
@Override
public Category createCategory(Integer aggregateId, CategoryDto categoryDto) {
return new SagaCategory(categoryDto);
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
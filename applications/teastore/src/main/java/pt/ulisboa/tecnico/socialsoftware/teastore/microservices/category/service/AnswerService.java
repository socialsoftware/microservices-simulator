package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.dao.CannotAcquireLockException;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.*;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.events.publish.CategoryUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.events.publish.CategoryDeletedEvent;

@Service
public class AnswerService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    private final CategoryRepository categoryRepository;

    @Autowired
    private CategoryFactory categoryFactory;

    public AnswerService(UnitOfWorkService unitOfWorkService, CategoryRepository categoryRepository) {
        this.unitOfWorkService = unitOfWorkService;
        this.categoryRepository = categoryRepository;
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CategoryDto createCategory(CategoryDto categoryDto, UnitOfWork unitOfWork) {
        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        Category category = categoryFactory.createCategory(aggregateId, categoryDto);
        unitOfWorkService.registerChanged(category, unitOfWork);
        return categoryFactory.createCategoryDto(category);
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CategoryDto getCategoryById(Integer aggregateId, UnitOfWork unitOfWork) {
        return categoryFactory.createCategoryDto((Category) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork));
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CategoryDto updateCategory(CategoryDto categoryDto, UnitOfWork unitOfWork) {
        Integer aggregateId = categoryDto.getAggregateId();
        Category oldCategory = (Category) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        Category newCategory = categoryFactory.createCategoryFromExisting(oldCategory);
        newCategory.setName(categoryDto.getName());
        newCategory.setDescription(categoryDto.getDescription());
        unitOfWorkService.registerChanged(newCategory, unitOfWork);
        unitOfWorkService.registerEvent(new CategoryUpdatedEvent(newCategory.getAggregateId(), newCategory.getName(), newCategory.getDescription()), unitOfWork);
        return categoryFactory.createCategoryDto(newCategory);
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteCategory(Integer aggregateId, UnitOfWork unitOfWork) {
        Category oldCategory = (Category) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        Category newCategory = categoryFactory.createCategoryFromExisting(oldCategory);
        newCategory.remove();
        unitOfWorkService.registerChanged(newCategory, unitOfWork);
        unitOfWorkService.registerEvent(new CategoryDeletedEvent(newCategory.getAggregateId()), unitOfWork);
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<CategoryDto> searchCategorys(String name, String description, UnitOfWork unitOfWork) {
        Set<Integer> aggregateIds = categoryRepository.findAll().stream()
                .filter(entity -> {
                    if (name != null) {
                        if (!entity.getName().equals(name)) {
                            return false;
                        }
                    }
                    if (description != null) {
                        if (!entity.getDescription().equals(description)) {
                            return false;
                        }
                    }
                    return true;
                })
                .map(Category::getAggregateId)
                .collect(Collectors.toSet());
        return aggregateIds.stream()
                .map(id -> (Category) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .map(categoryFactory::createCategoryDto)
                .collect(Collectors.toList());
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Category findCategoryById(Integer categoryAggregateId, UnitOfWork unitOfWork) {
        // TODO: Implement findCategoryById method
        return null;
    }

}

package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.CategoryDto;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.teastore.events.CategoryDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.teastore.events.CategoryUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.teastore.events.*;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.exception.TeastoreException;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.coordination.webapi.requestDtos.CreateCategoryRequestDto;
import org.springframework.context.ApplicationContext;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.aggregate.ProductRepository;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.aggregate.Product;


@Service
@Transactional(noRollbackFor = TeastoreException.class)
public class CategoryService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService unitOfWorkService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CategoryFactory categoryFactory;

    @Autowired
    private CategoryServiceExtension extension;

    @Autowired
    private ApplicationContext applicationContext;

    public CategoryService() {}

    public CategoryDto createCategory(CreateCategoryRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            CategoryDto categoryDto = new CategoryDto();
            categoryDto.setName(createRequest.getName());
            categoryDto.setDescription(createRequest.getDescription());

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Category category = categoryFactory.createCategory(aggregateId, categoryDto);
            unitOfWorkService.registerChanged(category, unitOfWork);
            return categoryFactory.createCategoryDto(category);
        } catch (TeastoreException e) {
            throw e;
        } catch (Exception e) {
            throw new TeastoreException("Error creating category: " + e.getMessage());
        }
    }

    public CategoryDto getCategoryById(Integer id, UnitOfWork unitOfWork) {
        try {
            Category category = (Category) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return categoryFactory.createCategoryDto(category);
        } catch (TeastoreException e) {
            throw e;
        } catch (Exception e) {
            throw new TeastoreException("Error retrieving category: " + e.getMessage());
        }
    }

    public List<CategoryDto> getAllCategorys(UnitOfWork unitOfWork) {
        try {
            Set<Integer> aggregateIds = categoryRepository.findAll().stream()
                .map(Category::getAggregateId)
                .collect(Collectors.toSet());

            return aggregateIds.stream()
                .map(id -> {
                    try {
                        return (Category) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .map(categoryFactory::createCategoryDto)
                .collect(Collectors.toList());
        } catch (TeastoreException e) {
            throw e;
        } catch (Exception e) {
            throw new TeastoreException("Error retrieving category: " + e.getMessage());
        }
    }

    public CategoryDto updateCategory(CategoryDto categoryDto, UnitOfWork unitOfWork) {
        try {
            Integer id = categoryDto.getAggregateId();
            Category oldCategory = (Category) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Category newCategory = categoryFactory.createCategoryFromExisting(oldCategory);
            if (categoryDto.getName() != null) {
                newCategory.setName(categoryDto.getName());
            }
            if (categoryDto.getDescription() != null) {
                newCategory.setDescription(categoryDto.getDescription());
            }

            unitOfWorkService.registerChanged(newCategory, unitOfWork);            CategoryUpdatedEvent event = new CategoryUpdatedEvent(newCategory.getAggregateId(), newCategory.getName(), newCategory.getDescription());
            event.setPublisherAggregateVersion(newCategory.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return categoryFactory.createCategoryDto(newCategory);
        } catch (TeastoreException e) {
            throw e;
        } catch (Exception e) {
            throw new TeastoreException("Error updating category: " + e.getMessage());
        }
    }

    public void deleteCategory(Integer id, UnitOfWork unitOfWork) {
        try {
            ProductRepository productRepositoryRef = applicationContext.getBean(ProductRepository.class);
            boolean hasProductReferences = productRepositoryRef.findAll().stream()
                .collect(Collectors.groupingBy(
                    Product::getAggregateId,
                    Collectors.maxBy(Comparator.comparingInt(Product::getVersion))))
                .values().stream()
                .flatMap(Optional::stream)
                .filter(s -> s.getState() != Category.AggregateState.DELETED)
                .anyMatch(s -> s.getProductCategory() != null && id.equals(s.getProductCategory().getCategoryAggregateId()));
            if (hasProductReferences) {
                throw new TeastoreException("Cannot delete category that has products");
            }
            Category oldCategory = (Category) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Category newCategory = categoryFactory.createCategoryFromExisting(oldCategory);
            newCategory.remove();
            unitOfWorkService.registerChanged(newCategory, unitOfWork);            unitOfWorkService.registerEvent(new CategoryDeletedEvent(newCategory.getAggregateId()), unitOfWork);
        } catch (TeastoreException e) {
            throw e;
        } catch (Exception e) {
            throw new TeastoreException("Error deleting category: " + e.getMessage());
        }
    }








}
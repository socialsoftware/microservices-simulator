package pt.ulisboa.tecnico.socialsoftware.teastore.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.service.ProductService;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.events.publish.CategoryUpdatedEvent;

@Service
public class ProductEventProcessing {
    @Autowired
    private ProductService productService;
    
    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public ProductEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }

    public void processCategoryUpdatedEvent(Integer aggregateId, CategoryUpdatedEvent categoryUpdatedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        productService.handleCategoryUpdatedEvent(aggregateId, categoryUpdatedEvent.getPublisherAggregateId(), categoryUpdatedEvent.getPublisherAggregateVersion(), unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }
}
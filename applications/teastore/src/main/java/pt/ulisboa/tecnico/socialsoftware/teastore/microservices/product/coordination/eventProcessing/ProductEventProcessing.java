package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.service.ProductService;
import pt.ulisboa.tecnico.socialsoftware.teastore.events.CategoryUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.teastore.events.CategoryDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.aggregate.Product;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.aggregate.ProductFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;

@Service
public class ProductEventProcessing {
    @Autowired
    private ProductService productService;

    @Autowired
    private ProductFactory productFactory;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public ProductEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }

    public void processCategoryUpdatedEvent(Integer aggregateId, CategoryUpdatedEvent categoryUpdatedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        productService.handleCategoryUpdatedEvent(aggregateId, categoryUpdatedEvent.getPublisherAggregateId(), categoryUpdatedEvent.getPublisherAggregateVersion(), categoryUpdatedEvent.getName(), categoryUpdatedEvent.getDescription(), unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processCategoryDeletedEvent(Integer aggregateId, CategoryDeletedEvent categoryDeletedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        Product oldProduct = (Product) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        Product newProduct = productFactory.createProductFromExisting(oldProduct);
        newProduct.setState(Aggregate.AggregateState.INACTIVE);
        unitOfWorkService.registerChanged(newProduct, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }
}
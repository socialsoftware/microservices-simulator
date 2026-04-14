package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.service.WishlistItemService;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.UserUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.ProductUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.aggregate.WishlistItem;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.aggregate.WishlistItemFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.UserDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.ProductDeletedEvent;

@Service
public class WishlistItemEventProcessing {
    @Autowired
    private WishlistItemService wishlistitemService;

    @Autowired
    private WishlistItemFactory wishlistitemFactory;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public WishlistItemEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }

    public void processUserUpdatedEvent(Integer aggregateId, UserUpdatedEvent userUpdatedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        wishlistitemService.handleUserUpdatedEvent(aggregateId, userUpdatedEvent.getPublisherAggregateId(), userUpdatedEvent.getPublisherAggregateVersion(), userUpdatedEvent.getUsername(), userUpdatedEvent.getEmail(), unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processProductUpdatedEvent(Integer aggregateId, ProductUpdatedEvent productUpdatedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        wishlistitemService.handleProductUpdatedEvent(aggregateId, productUpdatedEvent.getPublisherAggregateId(), productUpdatedEvent.getPublisherAggregateVersion(), productUpdatedEvent.getSku(), productUpdatedEvent.getName(), productUpdatedEvent.getPriceInCents(), unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processUserDeletedEvent(Integer aggregateId, UserDeletedEvent userDeletedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        WishlistItem oldWishlistItem = (WishlistItem) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        WishlistItem newWishlistItem = wishlistitemFactory.createWishlistItemFromExisting(oldWishlistItem);
        newWishlistItem.setState(Aggregate.AggregateState.INACTIVE);
        unitOfWorkService.registerChanged(newWishlistItem, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processProductDeletedEvent(Integer aggregateId, ProductDeletedEvent productDeletedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        WishlistItem oldWishlistItem = (WishlistItem) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        WishlistItem newWishlistItem = wishlistitemFactory.createWishlistItemFromExisting(oldWishlistItem);
        newWishlistItem.setState(Aggregate.AggregateState.INACTIVE);
        unitOfWorkService.registerChanged(newWishlistItem, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }
}
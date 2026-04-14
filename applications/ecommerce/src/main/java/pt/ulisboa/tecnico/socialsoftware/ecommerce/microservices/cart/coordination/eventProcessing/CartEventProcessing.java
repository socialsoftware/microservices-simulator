package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.service.CartService;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.UserUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.UserDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.aggregate.Cart;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.aggregate.CartFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;

@Service
public class CartEventProcessing {
    @Autowired
    private CartService cartService;

    @Autowired
    private CartFactory cartFactory;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public CartEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }

    public void processUserUpdatedEvent(Integer aggregateId, UserUpdatedEvent userUpdatedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        cartService.handleUserUpdatedEvent(aggregateId, userUpdatedEvent.getPublisherAggregateId(), userUpdatedEvent.getPublisherAggregateVersion(), userUpdatedEvent.getUsername(), userUpdatedEvent.getEmail(), unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processUserDeletedEvent(Integer aggregateId, UserDeletedEvent userDeletedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        Cart oldCart = (Cart) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        Cart newCart = cartFactory.createCartFromExisting(oldCart);
        newCart.setState(Aggregate.AggregateState.INACTIVE);
        unitOfWorkService.registerChanged(newCart, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }
}
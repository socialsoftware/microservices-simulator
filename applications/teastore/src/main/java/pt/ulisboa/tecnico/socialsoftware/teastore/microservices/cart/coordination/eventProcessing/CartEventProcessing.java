package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.cart.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.cart.service.CartService;

@Service
public class CartEventProcessing {
    @Autowired
    private CartService cartService;
    
    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public CartEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }}
package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.discount.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.discount.service.DiscountService;

@Service
public class DiscountEventProcessing {
    @Autowired
    private DiscountService discountService;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public DiscountEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }}
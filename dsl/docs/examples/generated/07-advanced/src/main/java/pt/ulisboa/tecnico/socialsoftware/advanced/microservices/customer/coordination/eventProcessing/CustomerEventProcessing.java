package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.customer.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.customer.service.CustomerService;

@Service
public class CustomerEventProcessing {
    @Autowired
    private CustomerService customerService;
    
    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public CustomerEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }}
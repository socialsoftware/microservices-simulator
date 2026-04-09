package com.example.dummyapp.order.coordination;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;

@Service
public class OrderFunctionalitiesFacade {

    @Autowired
    private SagaUnitOfWorkService sagaUnitOfWorkService;

    public void createOrder() {
        SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork("createOrder");
        CreateOrderFunctionalitySagas functionality =
                new CreateOrderFunctionalitySagas(sagaUnitOfWorkService, unitOfWork);
        functionality.executeWorkflow(unitOfWork);
    }
}

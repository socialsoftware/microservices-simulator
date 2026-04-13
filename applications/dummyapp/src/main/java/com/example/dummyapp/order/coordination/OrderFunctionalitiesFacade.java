package com.example.dummyapp.order.coordination;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWorkService;

@Service
public class OrderFunctionalitiesFacade {

    @Autowired
    private SagaUnitOfWorkService sagaUnitOfWorkService;

    public void createOrder(Integer customerId) {
        if (customerId == null) {
            SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork("createOrder");
            Integer customerIdCopy = customerId;
            CreateOrderFunctionalitySagas functionality =
                    new CreateOrderFunctionalitySagas(
                            sagaUnitOfWorkService,
                            unitOfWork,
                            customerId,
                            customerIdCopy);
            functionality.executeWorkflow(unitOfWork);
        } else {
            SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork("createOrder-shadow");
            unitOfWork.toString();
        }
    }
}

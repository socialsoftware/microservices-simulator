package pt.ulisboa.tecnico.socialsoftware.teastore.coordination.eventProcessing;

import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.teastore.microservices.exception.TeastoreErrorMessage.UNDEFINED_TRANSACTIONAL_MODEL;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.exception.TeastoreException;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.service.OrderService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;

@Service
public class OrderEventProcessing {
    @Autowired
    private OrderService orderService;
    
    @Autowired(required = false)
    private SagaUnitOfWorkService sagaUnitOfWorkService;

    @Autowired
    private Environment env;

    private TransactionalModel workflowType;

    @PostConstruct
    public void init() {
        String[] activeProfiles = env.getActiveProfiles();
        workflowType = Arrays.asList(activeProfiles).contains(SAGAS.getValue()) ? SAGAS : null;
        if (workflowType == null) {
            throw new TeastoreException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

{{eventProcessingMethods}}
}
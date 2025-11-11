package pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing;

import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersErrorMessage.UNDEFINED_TRANSACTIONAL_MODEL;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersException;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.service.ExecutionService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.events.publish.DeleteUserEvent;

@Service
public class ExecutionEventProcessing {
    @Autowired
    private ExecutionService executionService;
    
    @Autowired
    private UnitOfWorkService unitOfWorkService;
    
    @Autowired
    private Environment env;

    private TransactionalModel workflowType;

    @PostConstruct
    public void init() {
        String[] activeProfiles = env.getActiveProfiles();
        workflowType = Arrays.asList(activeProfiles).contains(SAGAS.getValue()) ? SAGAS : null;
        if (workflowType == null) {
            throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void processExecutionEvent(String eventType, Integer aggregateId, Integer aggregateVersion) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processDeleteUserEvent(Integer aggregateId, DeleteUserEvent deleteUserEvent) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        unitOfWorkService.commit(unitOfWork);
    }
}
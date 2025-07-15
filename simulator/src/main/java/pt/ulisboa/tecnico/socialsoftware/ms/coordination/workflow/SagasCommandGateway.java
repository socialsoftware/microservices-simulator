package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;


@Component
@Profile("sagas")
public class SagasCommandGateway extends CommandGateway {

    @Autowired
    private final SagaUnitOfWorkService sagaUnitOfWorkService;

    @Autowired
    public SagasCommandGateway(ApplicationContext applicationContext, SagaUnitOfWorkService sagaUnitOfWorkService) {
        super(applicationContext);
        this.sagaUnitOfWorkService = sagaUnitOfWorkService;
    }

    @Override
    public Object send(Command command) {
        if (command.getForbiddenStates() != null && !command.getForbiddenStates().isEmpty()) {
            sagaUnitOfWorkService.verifySagaState(command.getRootAggregateId(), command.getForbiddenStates());
        }

        Object response = super.send(command);

        if (command.getSemanticLock() != null) {
            sagaUnitOfWorkService.registerSagaState(command.getRootAggregateId(), command.getSemanticLock(), (SagaUnitOfWork) command.getUnitOfWork());
        }

        return response;
    }
}

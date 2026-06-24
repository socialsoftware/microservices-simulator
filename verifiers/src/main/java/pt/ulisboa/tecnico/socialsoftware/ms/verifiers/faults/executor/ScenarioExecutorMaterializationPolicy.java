package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

public final class ScenarioExecutorMaterializationPolicy {
    public static final String SAGA_UNIT_OF_WORK_SERVICE = "pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWorkService";
    public static final String SAGA_UNIT_OF_WORK = "pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWork";
    public static final String COMMAND_GATEWAY = "pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway";

    private ScenarioExecutorMaterializationPolicy() {
    }

    public static boolean isRuntimeOwned(String type) {
        return SAGA_UNIT_OF_WORK_SERVICE.equals(type)
                || COMMAND_GATEWAY.equals(type)
                || SAGA_UNIT_OF_WORK.equals(type);
    }
}

package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

public interface ScenarioRuntimeContext {
    Object bean(Class<?> type);

    default Object createSagaUnitOfWork(String functionalityName) {
        try {
            Class<?> type = Class.forName("pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork");
            return type.getConstructor(Long.class, String.class).newInstance(0L, functionalityName);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to create SagaUnitOfWork", e);
        }
    }
}

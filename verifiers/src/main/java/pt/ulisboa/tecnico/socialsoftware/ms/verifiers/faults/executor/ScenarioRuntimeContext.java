package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import java.util.List;

public interface ScenarioRuntimeContext {
    Object bean(Class<?> type);

    default <T> List<T> beans(Class<T> type) {
        return List.of();
    }

    default Class<?> resolveType(String persistedName) throws ClassNotFoundException {
        return Class.forName(persistedName);
    }

    default Class<?> resolveEventHandlingType(String persistedHandler, String processingMethod)
            throws ClassNotFoundException {
        return resolveType(persistedHandler);
    }

    default Object createSagaUnitOfWork(String functionalityName) {
        try {
            Class<?> type = Class.forName("pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork");
            return type.getConstructor(Long.class, String.class).newInstance(0L, functionalityName);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to create SagaUnitOfWork", e);
        }
    }
}

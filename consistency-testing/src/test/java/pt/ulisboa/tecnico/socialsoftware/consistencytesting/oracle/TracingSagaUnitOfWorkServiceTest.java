package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregateRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;

class TracingSagaUnitOfWorkServiceTest {

    @Test
    void ignoresConfiguredLockAndRecordsItsAcquisition() {
        TracingSagaUnitOfWorkService service = new TracingSagaUnitOfWorkService();
        SemanticLockId ignoredLock = SemanticLockId.from(TestSagaState.IGNORED);
        service.configureIgnoredSemanticLocks(Set.of(ignoredLock));

        SagaUnitOfWork unitOfWork = new SagaUnitOfWork(1L, "testFunctionality");
        unitOfWork.setCurrentExecutingStep("testStep");

        service.registerSagaState(17, TestSagaState.IGNORED, unitOfWork);

        assertTrue(unitOfWork.getPreviousStates().isEmpty());
        assertTrue(unitOfWork.getAggregatesInSaga().isEmpty());
        assertEquals(
                List.of(new IgnoredSemanticLockAcquisition(ignoredLock, 17, "testFunctionality", "testStep")),
                service.getIgnoredAcquisitions());
    }

    @Test
    void registersNonIgnoredSemanticLockUsingSuperclass() throws ReflectiveOperationException {
        TracingSagaUnitOfWorkService service = new TracingSagaUnitOfWorkService();
        service.configureIgnoredSemanticLocks(Set.of(SemanticLockId.from(TestSagaState.IGNORED)));

        SagaAggregateRepository repository = mock(SagaAggregateRepository.class);
        EntityManager entityManager = mock(EntityManager.class);
        Aggregate aggregate = mock(Aggregate.class, org.mockito.Mockito.withSettings()
                .extraInterfaces(SagaAggregate.class));
        SagaAggregate sagaAggregate = (SagaAggregate) aggregate;
        when(repository.findNonDeletedSagaAggregate(17)).thenReturn(Optional.of(aggregate));
        when(sagaAggregate.getSagaState()).thenReturn(GenericSagaState.NOT_IN_SAGA);
        setField(service, SagaUnitOfWorkService.class, "sagaAggregateRepository", repository);
        setField(service, SagaUnitOfWorkService.class, "entityManager", entityManager);

        SagaUnitOfWork unitOfWork = new SagaUnitOfWork(1L, "testFunctionality");
        service.registerSagaState(17, TestSagaState.NOT_IGNORED, unitOfWork);

        verify(sagaAggregate).setSagaState(TestSagaState.NOT_IGNORED);
        verify(entityManager).merge(aggregate);
        assertTrue(unitOfWork.getAggregatesInSaga().containsKey(17));
        assertEquals(GenericSagaState.NOT_IN_SAGA, unitOfWork.getPreviousStates().get(null).getFirst().state());
        assertTrue(service.getIgnoredAcquisitions().isEmpty());
    }

    @Test
    void usesDeclaringEnumClassForConstantSpecificEnumImplementations() {
        SemanticLockId lock = SemanticLockId.from(TestSagaState.IGNORED);

        assertEquals(TestSagaState.class.getName(), lock.sagaStateClassName());
        assertEquals("IGNORED", lock.stateName());
        // A constant-specific enum class would be named TestSagaState$1.
        assertFalse(lock.sagaStateClassName().endsWith("$1"));
    }

    @Test
    void ignoredRegistrationDoesNotAccessPersistence() throws ReflectiveOperationException {
        TracingSagaUnitOfWorkService service = new TracingSagaUnitOfWorkService();
        service.configureIgnoredSemanticLocks(Set.of(SemanticLockId.from(TestSagaState.IGNORED)));
        SagaAggregateRepository repository = mock(SagaAggregateRepository.class);
        setField(service, SagaUnitOfWorkService.class, "sagaAggregateRepository", repository);

        service.registerSagaState(17, TestSagaState.IGNORED, new SagaUnitOfWork(1L, "testFunctionality"));

        verifyNoInteractions(repository);
    }

    private static void setField(Object target, Class<?> declaringClass, String fieldName, Object value)
            throws ReflectiveOperationException {

        Field field = declaringClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private enum TestSagaState implements SagaState {
        IGNORED {
            @Override
            public String getStateName() {
                return "IGNORED";
            }
        },
        NOT_IGNORED {
            @Override
            public String getStateName() {
                return "NOT_IGNORED";
            }
        }
    }
}

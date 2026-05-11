package pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic.DynamicEvidenceContext;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic.DynamicEvidenceEvent;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic.DynamicEvidenceNoopRecorder;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic.DynamicEvidenceRecorder;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic.DynamicEvidenceRecorderHolder;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.aggregate.SagaAggregateRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.version.IVersionService;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SagaUnitOfWorkServiceDynamicEvidenceTest {

    @AfterEach
    void tearDown() {
        DynamicEvidenceRecorderHolder.setRecorder(new DynamicEvidenceNoopRecorder());
        DynamicEvidenceContext.clear();
    }

    @Test
    void aggregateLoadAndRegisterReadEmitsReadEventWithUnitOfWorkFallbackContext() {
        RecordingRecorder recorder = new RecordingRecorder();
        DynamicEvidenceRecorderHolder.setRecorder(recorder);

        SagaAggregateRepository repository = mock(SagaAggregateRepository.class);
        TestAggregate aggregate = new TestAggregate(2, "SagaExecution");
        when(repository.findNonDeletedSagaAggregate(2)).thenReturn(Optional.of(aggregate));
        SagaUnitOfWorkService service = serviceWith(repository, mock(EntityManager.class), mock(IVersionService.class));
        SagaUnitOfWork unitOfWork = new SagaUnitOfWork(90L, "CreateTournamentFunctionalitySagas");

        Aggregate loaded = service.aggregateLoadAndRegisterRead(2, unitOfWork);

        assertThat(loaded).isSameAs(aggregate);
        assertThat(recorder.events).hasSize(1);
        DynamicEvidenceEvent event = recorder.events.getFirst();
        assertThat(event.getEventKind()).isEqualTo("AGGREGATE_ACCESSED");
        assertThat(event.getFunctionalityName()).isEqualTo("CreateTournamentFunctionalitySagas");
        assertThat(event.getFunctionalityInvocationId()).isEqualTo("CreateTournamentFunctionalitySagas-90");
        assertThat(event.getStepName()).isNull();
        assertThat(event.getUnitOfWorkVersion()).isEqualTo(90L);
        assertThat(event.getPayload()).containsEntry("accessMode", "READ")
                .containsEntry("aggregateType", "SagaExecution")
                .containsEntry("aggregateId", "2")
                .containsEntry("sourceMethod", "SagaUnitOfWorkService.aggregateLoadAndRegisterRead");
    }

    @Test
    void aggregateLoadAndRegisterReadDoesNotEmitEventWhenLoadFails() {
        RecordingRecorder recorder = new RecordingRecorder();
        DynamicEvidenceRecorderHolder.setRecorder(recorder);

        SagaAggregateRepository repository = mock(SagaAggregateRepository.class);
        when(repository.findNonDeletedSagaAggregate(404)).thenReturn(Optional.empty());
        SagaUnitOfWorkService service = serviceWith(repository, mock(EntityManager.class), mock(IVersionService.class));

        assertThatThrownBy(() -> service.aggregateLoadAndRegisterRead(404, new SagaUnitOfWork(1L, "missing")))
                .isInstanceOf(SimulatorException.class);
        assertThat(recorder.events).isEmpty();
    }

    @Test
    void registerChangedEmitsWriteEventWithCurrentStepContextAfterSuccessfulMerge() {
        RecordingRecorder recorder = new RecordingRecorder();
        DynamicEvidenceRecorderHolder.setRecorder(recorder);

        EntityManager entityManager = mock(EntityManager.class);
        IVersionService versionService = mock(IVersionService.class);
        when(versionService.incrementAndGetVersionNumber()).thenReturn(121L);
        SagaUnitOfWorkService service = serviceWith(mock(SagaAggregateRepository.class), entityManager, versionService);
        TestAggregate aggregate = new TestAggregate(2, "SagaExecution");
        SagaUnitOfWork unitOfWork = new SagaUnitOfWork(90L, "fallbackFunctionality");

        try (DynamicEvidenceContext.Scope ignored = DynamicEvidenceContext.enterStep(
                "CreateTournamentFunctionalitySagas", "getCourseExecutionStep", 307L)) {
            service.registerChanged(aggregate, unitOfWork);
        }

        verify(entityManager).merge(aggregate);
        assertThat(aggregate.invariantsVerified).isTrue();
        assertThat(unitOfWork.getVersion()).isEqualTo(121L);
        assertThat(recorder.events).hasSize(1);
        DynamicEvidenceEvent event = recorder.events.getFirst();
        assertThat(event.getEventKind()).isEqualTo("AGGREGATE_ACCESSED");
        assertThat(event.getFunctionalityName()).isEqualTo("CreateTournamentFunctionalitySagas");
        assertThat(event.getFunctionalityInvocationId()).isEqualTo("CreateTournamentFunctionalitySagas-307");
        assertThat(event.getStepName()).isEqualTo("getCourseExecutionStep");
        assertThat(event.getUnitOfWorkVersion()).isEqualTo(307L);
        assertThat(event.getPayload()).containsEntry("accessMode", "WRITE")
                .containsEntry("aggregateType", "SagaExecution")
                .containsEntry("aggregateId", "2")
                .containsEntry("sourceMethod", "SagaUnitOfWorkService.registerChanged");
    }

    @Test
    void aggregateAccessRecorderFailuresDoNotBreakReadOrWriteOperations() {
        DynamicEvidenceRecorderHolder.setRecorder(new ThrowingRecorder());

        SagaAggregateRepository repository = mock(SagaAggregateRepository.class);
        TestAggregate aggregate = new TestAggregate(2, "SagaExecution");
        when(repository.findNonDeletedSagaAggregate(2)).thenReturn(Optional.of(aggregate));
        SagaUnitOfWorkService service = serviceWith(repository, mock(EntityManager.class), mock(IVersionService.class));
        SagaUnitOfWork readUnitOfWork = new SagaUnitOfWork(90L, "CreateTournamentFunctionalitySagas");

        assertThat(service.aggregateLoadAndRegisterRead(2, readUnitOfWork)).isSameAs(aggregate);

        EntityManager entityManager = mock(EntityManager.class);
        IVersionService versionService = mock(IVersionService.class);
        when(versionService.incrementAndGetVersionNumber()).thenReturn(121L);
        SagaUnitOfWorkService writeService = serviceWith(mock(SagaAggregateRepository.class), entityManager, versionService);
        TestAggregate writeAggregate = new TestAggregate(3, "SagaExecution");

        assertThatCode(() -> writeService.registerChanged(writeAggregate, new SagaUnitOfWork(90L, "write")))
                .doesNotThrowAnyException();
        verify(entityManager).merge(writeAggregate);
    }

    @Test
    void registerChangedDoesNotEmitWriteEventWhenMergeFails() {
        RecordingRecorder recorder = new RecordingRecorder();
        DynamicEvidenceRecorderHolder.setRecorder(recorder);

        EntityManager entityManager = mock(EntityManager.class);
        IVersionService versionService = mock(IVersionService.class);
        when(versionService.incrementAndGetVersionNumber()).thenReturn(121L);
        TestAggregate aggregate = new TestAggregate(2, "SagaExecution");
        doThrow(new RuntimeException("merge failed")).when(entityManager).merge(aggregate);
        SagaUnitOfWorkService service = serviceWith(mock(SagaAggregateRepository.class), entityManager, versionService);

        assertThatThrownBy(() -> service.registerChanged(aggregate, new SagaUnitOfWork(90L, "write")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("merge failed");
        assertThat(recorder.events).isEmpty();
    }

    private SagaUnitOfWorkService serviceWith(SagaAggregateRepository repository,
                                              EntityManager entityManager,
                                              IVersionService versionService) {
        SagaUnitOfWorkService service = new SagaUnitOfWorkService();
        ReflectionTestUtils.setField(service, "sagaAggregateRepository", repository);
        ReflectionTestUtils.setField(service, "entityManager", entityManager);
        ReflectionTestUtils.setField(service, "versionService", versionService);
        return service;
    }

    private static class TestAggregate extends Aggregate {
        private boolean invariantsVerified;

        TestAggregate(Integer aggregateId, String aggregateType) {
            super(aggregateId);
            setAggregateType(aggregateType);
        }

        @Override
        public void verifyInvariants() {
            invariantsVerified = true;
        }

        @Override
        public Set<EventSubscription> getEventSubscriptions() {
            return Set.of();
        }
    }

    private static class RecordingRecorder implements DynamicEvidenceRecorder {
        private final List<DynamicEvidenceEvent> events = new CopyOnWriteArrayList<>();

        @Override
        public void record(DynamicEvidenceEvent event) {
            events.add(event);
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public void close() {
            // no-op for tests
        }
    }

    private static class ThrowingRecorder implements DynamicEvidenceRecorder {
        @Override
        public void record(DynamicEvidenceEvent event) {
            throw new RuntimeException("recorder failed");
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public void close() {
            // no-op for tests
        }
    }
}

package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventHandling;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

class ScheduleExecutorDeferredEventRetryTest {

    private static final String PUBLISH_STEP = "publishStep";
    private static final String UNLOCK_STEP = "unlockStep";

    @Test
    void retriesPendingEventAfterLaterFunctionalityStepWithoutBusyLooping() {
        DeferredEventApplicationService eventService = new DeferredEventApplicationService();
        RetryingEventHandler handler = new RetryingEventHandler();
        TestEvent event = new TestEvent();
        event.setId(10);
        event.setPublisherAggregateId(20);
        PollingEventHandling polling = new PollingEventHandling(eventService, handler, event, 30);

        SagaUnitOfWorkService unitOfWorkService = new NoOpSagaUnitOfWorkService();
        DeferredEventRetryFunctionality functionality = new DeferredEventRetryFunctionality(
                unitOfWorkService, handler);
        FunctionalityId functionalityId = FunctionalityId.forSagaFunctionality("deferredEventRetry");

        StepId publishStepId = StepId.forFunctionalityStep(functionalityId, PUBLISH_STEP);
        StepId unlockStepId = StepId.forFunctionalityStep(functionalityId, UNLOCK_STEP);
        StepId firstAttemptId = eventStepId(event, handler, publishStepId, 30);
        StepId retryAttemptId = eventStepId(event, handler, unlockStepId, 30);

        StepDependencies dependencies = new StepDependencies()
                .addStepDependencies(unlockStepId, Set.of(firstAttemptId))
                .addStepDependencies(StepId.forCommitStep(functionalityId), Set.of(retryAttemptId));

        TracingSagaUnitOfWorkService tracingService = new TracingSagaUnitOfWorkService();
        try (DeferredEventApplicationService.CaptureSession captureSession = eventService.beginCapture();
                TracingSagaUnitOfWorkService.TraceSession traceSession = tracingService.beginTrace()) {
            ScheduleExecutor executor = new ScheduleExecutor(
                    Map.of(functionalityId, functionality),
                    Set.of(),
                    dependencies,
                    unitOfWorkService,
                    traceSession,
                    captureSession,
                    Set.of(polling),
                    42L);

            TestResult result = executor.execute();

            assertTrue(handler.processed);
            assertEquals(2, handler.attemptCount,
                    "The handler should wait for progress instead of retrying itself in a busy loop");
            assertTrue(result.schedule().indexOf(firstAttemptId) < result.schedule().indexOf(unlockStepId));
            assertTrue(result.schedule().indexOf(unlockStepId) < result.schedule().indexOf(retryAttemptId));
            assertTrue(result.statuses().isEmpty());
        }
    }

    private static StepId eventStepId(
            Event event,
            EventHandler handler,
            StepId emittingStepId,
            Integer subscriberAggregateId) {
        FunctionalityId eventFunctionalityId = FunctionalityId.forEventHandlerFunctionality(
                event.getClass(), handler.getClass(), emittingStepId, subscriberAggregateId,
                event.getPublisherAggregateId());
        return StepId.forEventHandlerStep(eventFunctionalityId);
    }

    private static final class DeferredEventRetryFunctionality extends WorkflowFunctionality {
        private final RetryingEventHandler handler;

        private DeferredEventRetryFunctionality(
                SagaUnitOfWorkService unitOfWorkService,
                RetryingEventHandler handler) {
            this.handler = handler;
            SagaUnitOfWork unitOfWork = new SagaUnitOfWork(1L, "deferredEventRetry");
            workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

            SagaStep publishStep = new SagaStep(PUBLISH_STEP, () -> {
            });
            SagaStep unlockStep = new SagaStep(
                    UNLOCK_STEP,
                    () -> handler.unlocked = true,
                    new ArrayList<>(List.of(publishStep)));

            workflow.addStep(publishStep);
            workflow.addStep(unlockStep);
        }

        @Override
        public void executeUntilStep(String stepName, UnitOfWork unitOfWork) {
            if (UNLOCK_STEP.equals(stepName)) {
                handler.unlocked = true;
            }
        }
    }

    private static final class PollingEventHandling implements EventHandling {
        private final DeferredEventApplicationService eventService;
        private final RetryingEventHandler handler;
        private final TestEvent event;
        private final Integer subscriberAggregateId;

        private PollingEventHandling(
                DeferredEventApplicationService eventService,
                RetryingEventHandler handler,
                TestEvent event,
                Integer subscriberAggregateId) {
            this.eventService = eventService;
            this.handler = handler;
            this.event = event;
            this.subscriberAggregateId = subscriberAggregateId;
        }

        @Scheduled(fixedDelay = 1000)
        public void poll() {
            if (!handler.processed) {
                eventService.dispatchToHandler(handler, subscriberAggregateId, event);
            }
        }
    }

    private static final class RetryingEventHandler extends EventHandler {
        private boolean unlocked;
        private boolean processed;
        private int attemptCount;

        private RetryingEventHandler() {
            super(null);
        }

        @Override
        public void handleEvent(Integer subscriberAggregateId, Event event) {
            attemptCount++;
            if (unlocked) {
                processed = true;
            }
        }
    }

    private static final class TestEvent extends Event {
    }

    private static final class NoOpSagaUnitOfWorkService extends SagaUnitOfWorkService {
        @Override
        public void commit(SagaUnitOfWork unitOfWork) {
        }
    }
}

package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

class EventHandlerStepIdentityTest {

    private static final StepId CAPTURE_STEP = StepId.forFunctionalityStep(
            FunctionalityId.forSagaFunctionality("publisher"), "publishStep");

    @Test
    void differentPersistedEventsProduceAndExecuteDifferentHandlerSteps() {
        DeferredEventApplicationService service = new DeferredEventApplicationService();
        CountingHandler handler = new CountingHandler();
        TestEvent first = event(10);
        TestEvent second = event(11);

        List<EventHandlerStep> steps = captureSteps(service,
                List.of(new Delivery(first, handler, 30), new Delivery(second, handler, 30)));

        assertEquals(2, steps.size());
        assertNotEquals(steps.get(0).getId(), steps.get(1).getId());
        assertNotEquals(steps.get(0).getFunctionalityId(), steps.get(1).getFunctionalityId());
        assertEquals(Set.of(10, 11), steps.stream().map(EventHandlerStep::eventId).collect(Collectors.toSet()));
        assertTrue(steps.stream().anyMatch(step -> step.getId().toString().contains("eventId-0000000010")));
        assertTrue(steps.stream().anyMatch(step -> step.getId().toString().contains("eventId-0000000011")));

        steps.forEach(EventHandlerStep::execute);
        assertEquals(2, handler.deliveries.size());
    }

    @Test
    void scheduleExecutorAddsAndExecutesBothDistinctDeliveriesFromOnePoll() {
        DeferredEventApplicationService eventService = new DeferredEventApplicationService();
        CountingHandler handler = new CountingHandler();
        OneShotPolling polling = new OneShotPolling(eventService, handler, List.of(event(10), event(11)), 30);
        SagaUnitOfWorkService unitOfWorkService = new NoOpSagaUnitOfWorkService();
        FunctionalityId functionalityId = FunctionalityId.forSagaFunctionality("publisher");
        WorkflowFunctionality functionality = new OneStepFunctionality(unitOfWorkService);
        TracingSagaUnitOfWorkService tracingService = new TracingSagaUnitOfWorkService();

        try (DeferredEventApplicationService.CaptureSession captureSession = eventService.beginCapture();
                TracingSagaUnitOfWorkService.TraceSession traceSession = tracingService.beginTrace()) {

            TestResult result = new ScheduleExecutor(
                    Map.of(functionalityId, functionality), Set.of(), new StepDependencies(),
                    unitOfWorkService, traceSession, captureSession,
                    Set.of(polling), 42L).execute();

            assertEquals(2, handler.deliveries.size());
            assertEquals(1, result.schedule().stream()
                    .filter(step -> step.toString().contains("eventId-0000000010"))
                    .count());
            assertEquals(1, result.schedule().stream()
                    .filter(step -> step.toString().contains("eventId-0000000011"))
                    .count());
            assertTrue(result.statuses().isEmpty());
        }
    }

    @Test
    void schedulerPreservesForcedOrderingAroundEventHandlerStep() {
        DeferredEventApplicationService eventService = new DeferredEventApplicationService();
        CountingHandler handler = new CountingHandler();
        TestEvent event = event(10);
        OneShotPolling polling = new OneShotPolling(eventService, handler, List.of(event), 30);
        SagaUnitOfWorkService unitOfWorkService = new NoOpSagaUnitOfWorkService();

        FunctionalityId publisherId = FunctionalityId.forSagaFunctionality("publisher");
        FunctionalityId orderingId = FunctionalityId.forSagaFunctionality("ordering");
        WorkflowFunctionality publisher = new OneStepFunctionality(unitOfWorkService);
        WorkflowFunctionality ordering = new TwoStepFunctionality(unitOfWorkService);

        StepId publishStepId = StepId.forFunctionalityStep(publisherId, "publishStep");
        StepId beforeEventStepId = StepId.forFunctionalityStep(orderingId, "beforeEvent");
        StepId eventStepId = eventStepId(event, handler, publishStepId, 30);
        StepId afterEventStepId = StepId.forFunctionalityStep(orderingId, "afterEvent");
        StepDependencies dependencies = new StepDependencies()
                .addStepDependencies(eventStepId, Set.of(beforeEventStepId))
                .addStepDependencies(afterEventStepId, Set.of(eventStepId));

        TracingSagaUnitOfWorkService tracingService = new TracingSagaUnitOfWorkService();
        try (DeferredEventApplicationService.CaptureSession captureSession = eventService.beginCapture();
                TracingSagaUnitOfWorkService.TraceSession traceSession = tracingService.beginTrace()) {

            TestResult result = new ScheduleExecutor(
                    Map.of(publisherId, publisher, orderingId, ordering), Set.of(), dependencies,
                    unitOfWorkService, traceSession, captureSession,
                    Set.of(polling), 42L).execute();

            assertTrue(result.schedule().indexOf(beforeEventStepId) < result.schedule().indexOf(eventStepId));
            assertTrue(result.schedule().indexOf(eventStepId) < result.schedule().indexOf(afterEventStepId));
            assertEquals(1, handler.deliveries.size());
            assertTrue(result.statuses().isEmpty());
        }
    }

    @Test
    void oneEventToDifferentSubscribersProducesDifferentSteps() {
        DeferredEventApplicationService service = new DeferredEventApplicationService();
        CountingHandler handler = new CountingHandler();
        TestEvent event = event(10);

        List<EventHandlerStep> steps = captureSteps(service,
                List.of(new Delivery(event, handler, 30), new Delivery(event, handler, 31)));

        assertEquals(2, steps.size());
        assertNotEquals(steps.get(0).getId(), steps.get(1).getId());
    }

    @Test
    void oneEventThroughDifferentHandlersProducesDifferentSteps() {
        DeferredEventApplicationService service = new DeferredEventApplicationService();
        CountingHandler firstHandler = new CountingHandler();
        OtherCountingHandler secondHandler = new OtherCountingHandler();
        TestEvent event = event(10);

        List<EventHandlerStep> steps = captureSteps(service,
                List.of(new Delivery(event, firstHandler, 30), new Delivery(event, secondHandler, 30)));

        assertEquals(2, steps.size());
        assertNotEquals(steps.get(0).getId(), steps.get(1).getId());
    }

    @Test
    void eventIdEncodingPreservesNumericOrderForReadySetSorting() {
        DeferredEventApplicationService service = new DeferredEventApplicationService();
        CountingHandler handler = new CountingHandler();
        List<EventHandlerStep> steps = captureSteps(service,
                List.of(new Delivery(event(2), handler, 30), new Delivery(event(10), handler, 30)));

        String firstId = steps.stream()
                .filter(step -> step.getId().toString().contains("eventId-0000000002"))
                .findFirst().orElseThrow().getId().toString();
        String secondId = steps.stream()
                .filter(step -> step.getId().toString().contains("eventId-0000000010"))
                .findFirst().orElseThrow().getId().toString();

        assertTrue(firstId.compareTo(secondId) < 0);
    }

    private static List<EventHandlerStep> captureSteps(
            DeferredEventApplicationService service, List<Delivery> deliveries) {

        try (DeferredEventApplicationService.CaptureSession session = service.beginCapture()) {
            for (Delivery delivery : deliveries) {
                service.dispatchToHandler(delivery.handler(), delivery.subscriberAggregateId(), delivery.event());
            }

            return session.drain().stream()
                    .map(invocation -> new EventHandlerStep(invocation, CAPTURE_STEP))
                    .toList();
        }
    }

    private static TestEvent event(int id) {
        TestEvent event = new TestEvent();
        event.setId(id);
        event.setPublisherAggregateId(20);
        return event;
    }

    private static StepId eventStepId(
            Event event,
            EventHandler handler,
            StepId capturedAfterStepId,
            Integer subscriberAggregateId) {

        DeferredEventInvocation invocation = new DeferredEventInvocation(
                event, handler, subscriberAggregateId, () -> {
                });

        FunctionalityId eventFunctionalityId = FunctionalityId.forEventHandlerFunctionality(
                invocation, capturedAfterStepId);
        return StepId.forEventHandlerStep(eventFunctionalityId);
    }

    private record Delivery(Event event, EventHandler handler, Integer subscriberAggregateId) {
    }

    private static class CountingHandler extends EventHandler {
        private final List<Integer> deliveries = new ArrayList<>();

        private CountingHandler() {
            super(null);
        }

        @Override
        public void handleEvent(Integer subscriberAggregateId, Event event) {
            deliveries.add(subscriberAggregateId);
        }
    }

    private static final class OtherCountingHandler extends CountingHandler {
    }

    private static final class TestEvent extends Event {
    }

    private static final class OneShotPolling implements EventHandling {
        private final DeferredEventApplicationService eventService;
        private final EventHandler handler;
        private final List<? extends Event> events;
        private final Integer subscriberAggregateId;
        private boolean hasPolled;

        private OneShotPolling(
                DeferredEventApplicationService eventService,
                EventHandler handler,
                List<? extends Event> events,
                Integer subscriberAggregateId) {
            this.eventService = eventService;
            this.handler = handler;
            this.events = events;
            this.subscriberAggregateId = subscriberAggregateId;
        }

        @Scheduled(fixedDelay = 1000)
        public void poll() {
            if (hasPolled) {
                return;
            }
            hasPolled = true;
            events.forEach(event -> eventService.dispatchToHandler(handler, subscriberAggregateId, event));
        }
    }

    private static final class OneStepFunctionality extends WorkflowFunctionality {
        private OneStepFunctionality(SagaUnitOfWorkService unitOfWorkService) {
            SagaUnitOfWork unitOfWork = new SagaUnitOfWork(1L, "publisher");
            workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);
            workflow.addStep(new SagaStep("publishStep", () -> {
            }));
        }

        @Override
        public void executeUntilStep(String stepName, UnitOfWork unitOfWork) {
        }
    }

    private static final class TwoStepFunctionality extends WorkflowFunctionality {
        private TwoStepFunctionality(SagaUnitOfWorkService unitOfWorkService) {
            SagaUnitOfWork unitOfWork = new SagaUnitOfWork(2L, "ordering");
            workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);
            SagaStep beforeEvent = new SagaStep("beforeEvent", () -> {
            });
            SagaStep afterEvent = new SagaStep("afterEvent", () -> {
            }, new ArrayList<>(List.of(beforeEvent)));
            workflow.addStep(beforeEvent);
            workflow.addStep(afterEvent);
        }

        @Override
        public void executeUntilStep(String stepName, UnitOfWork unitOfWork) {
        }
    }

    private static final class NoOpSagaUnitOfWorkService extends SagaUnitOfWorkService {
        @Override
        public void commit(SagaUnitOfWork unitOfWork) {
        }
    }
}

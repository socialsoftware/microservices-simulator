package pt.ulisboa.tecnico.socialsoftware.ms.coordination;

import io.opentelemetry.api.trace.Span;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.TraceManager;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic.DynamicEvidenceContext;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic.DynamicEvidenceEvent;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic.DynamicEvidenceNoopRecorder;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic.DynamicEvidenceRecorder;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic.DynamicEvidenceRecorderHolder;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ExecutionPlanDynamicEvidenceTest {
    @AfterEach
    void resetRecorder() {
        DynamicEvidenceRecorderHolder.setRecorder(new DynamicEvidenceNoopRecorder());
        DynamicEvidenceContext.clear();
        clearTraceManagerState();
        Thread.interrupted();
    }

    @Test
    void emitsStepStartedAndFinishedWithFunctionalityAndStepContext() {
        RecordingRecorder recorder = new RecordingRecorder();
        DynamicEvidenceRecorderHolder.setRecorder(recorder);
        TestUnitOfWork unitOfWork = new TestUnitOfWork(41L, "checkout");
        AtomicReference<DynamicEvidenceContext.StepContext> contextDuringStep = new AtomicReference<>();
        FlowStep reserveStock = new TestStep("reserveStock", () -> {
            contextDuringStep.set(DynamicEvidenceContext.current().orElseThrow());
            assertThat(DynamicEvidenceContext.currentFunctionalityName()).contains("checkout");
            assertThat(DynamicEvidenceContext.currentFunctionalityClassFqn()).contains(TestFunctionality.class.getName());
            assertThat(DynamicEvidenceContext.currentStepName()).contains("reserveStock");
        });
        ArrayList<FlowStep> plan = new ArrayList<>(List.of(reserveStock));
        HashMap<FlowStep, ArrayList<FlowStep>> dependencies = new HashMap<>();
        dependencies.put(reserveStock, new ArrayList<>());

        new ExecutionPlan(plan, dependencies, new TestFunctionality()).execute(unitOfWork).join();

        assertThat(recorder.events).extracting(DynamicEvidenceEvent::getEventKind)
                .containsExactly("STEP_STARTED", "STEP_FINISHED");
        assertThat(recorder.events).allSatisfy(event -> {
            assertThat(event.getFunctionalityName()).isEqualTo("checkout");
            assertThat(event.getFunctionalityClassFqn()).isEqualTo(TestFunctionality.class.getName());
            assertThat(event.getFunctionalityClassSimpleName()).isEqualTo(TestFunctionality.class.getSimpleName());
            assertThat(event.getStepName()).isEqualTo("reserveStock");
            assertThat(event.getUnitOfWorkVersion()).isEqualTo(41L);
        });
        assertThat(recorder.events.getFirst().getPayload()).containsEntry("stepPhase", "FORWARD");
        assertThat(recorder.events.get(1).getPayload()).containsEntry("outcome", "SUCCESS");
        assertThat(recorder.events.get(1).getPayload()).containsKey("durationMillis");
        assertThat(contextDuringStep.get().functionalityName()).isEqualTo("checkout");
        assertThat(contextDuringStep.get().functionalityClassFqn()).isEqualTo(TestFunctionality.class.getName());
        assertThat(contextDuringStep.get().functionalityClassSimpleName()).isEqualTo(TestFunctionality.class.getSimpleName());
        assertThat(contextDuringStep.get().stepName()).isEqualTo("reserveStock");
        assertThat(contextDuringStep.get().unitOfWorkVersion()).isEqualTo(41L);
        assertThat(DynamicEvidenceContext.current()).isEmpty();
    }

    @Test
    void emitsStepFinishedWithErrorWhenStepFutureFailsAndClearsContext() {
        RecordingRecorder recorder = new RecordingRecorder();
        DynamicEvidenceRecorderHolder.setRecorder(recorder);
        TestUnitOfWork unitOfWork = new TestUnitOfWork(42L, "checkout");
        IllegalStateException failure = new IllegalStateException("boom");
        FlowStep reserveStock = new TestStep("reserveStock", () -> {
            throw failure;
        });
        ArrayList<FlowStep> plan = new ArrayList<>(List.of(reserveStock));
        HashMap<FlowStep, ArrayList<FlowStep>> dependencies = new HashMap<>();
        dependencies.put(reserveStock, new ArrayList<>());

        CompletableFuture<Void> execution = new ExecutionPlan(plan, dependencies, new TestFunctionality()).execute(unitOfWork);

        assertThatThrownBy(execution::join)
                .isInstanceOf(CompletionException.class)
                .hasCause(failure);
        assertThat(recorder.events).extracting(DynamicEvidenceEvent::getEventKind)
                .containsExactly("STEP_STARTED", "STEP_FINISHED");
        DynamicEvidenceEvent finished = recorder.events.get(1);
        assertThat(finished.getPayload()).containsEntry("outcome", "ERROR");
        assertThat(finished.getPayload()).containsKey("durationMillis");
        assertThat(finished.getPayload()).containsEntry("errorType", IllegalStateException.class.getName());
        assertThat(finished.getPayload()).containsEntry("errorMessage", "boom");
        assertThat(DynamicEvidenceContext.current()).isEmpty();
    }

    @Test
    void successfulStepStillSucceedsWhenRecorderThrowsOnStepStartedAndFinished() {
        ThrowingRecorder recorder = new ThrowingRecorder(Set.of("STEP_STARTED", "STEP_FINISHED"));
        DynamicEvidenceRecorderHolder.setRecorder(recorder);
        TestUnitOfWork unitOfWork = new TestUnitOfWork(47L, "checkout");
        AtomicBoolean stepExecuted = new AtomicBoolean(false);
        FlowStep reserveStock = new TestStep("reserveStock", () -> stepExecuted.set(true));
        ArrayList<FlowStep> plan = new ArrayList<>(List.of(reserveStock));
        HashMap<FlowStep, ArrayList<FlowStep>> dependencies = new HashMap<>();
        dependencies.put(reserveStock, new ArrayList<>());

        new ExecutionPlan(plan, dependencies, new TestFunctionality()).execute(unitOfWork).join();

        assertThat(stepExecuted.get()).isTrue();
        assertThat(recorder.attemptedEventKinds).containsExactly("STEP_STARTED", "STEP_FINISHED");
    }

    @Test
    void failingStepStillFailsWithOriginalCauseWhenRecorderThrowsOnStepFinished() {
        ThrowingRecorder recorder = new ThrowingRecorder(Set.of("STEP_FINISHED"));
        DynamicEvidenceRecorderHolder.setRecorder(recorder);
        TestUnitOfWork unitOfWork = new TestUnitOfWork(48L, "checkout");
        IllegalStateException failure = new IllegalStateException("boom");
        FlowStep reserveStock = new TestStep("reserveStock", () -> {
            throw failure;
        });
        ArrayList<FlowStep> plan = new ArrayList<>(List.of(reserveStock));
        HashMap<FlowStep, ArrayList<FlowStep>> dependencies = new HashMap<>();
        dependencies.put(reserveStock, new ArrayList<>());

        CompletableFuture<Void> execution = new ExecutionPlan(plan, dependencies, new TestFunctionality()).execute(unitOfWork);

        assertThatThrownBy(execution::join)
                .isInstanceOf(CompletionException.class)
                .satisfies(throwable -> assertThat(throwable.getCause()).isSameAs(failure));
        assertThat(recorder.attemptedEventKinds).containsExactly("STEP_STARTED", "STEP_FINISHED");
    }

    @Test
    void emitsSingleStepFinishedWhenAsyncFutureCompletesAndClearsContext() {
        RecordingRecorder recorder = new RecordingRecorder();
        DynamicEvidenceRecorderHolder.setRecorder(recorder);
        TestUnitOfWork unitOfWork = new TestUnitOfWork(43L, "checkout");
        CompletableFuture<Void> stepFuture = new CompletableFuture<>();
        AtomicReference<DynamicEvidenceContext.StepContext> contextDuringStep = new AtomicReference<>();
        FlowStep reserveStock = new AsyncTestStep("reserveStock", () -> {
            contextDuringStep.set(DynamicEvidenceContext.current().orElseThrow());
            return stepFuture;
        });
        ArrayList<FlowStep> plan = new ArrayList<>(List.of(reserveStock));
        HashMap<FlowStep, ArrayList<FlowStep>> dependencies = new HashMap<>();
        dependencies.put(reserveStock, new ArrayList<>());

        CompletableFuture<Void> execution = new ExecutionPlan(plan, dependencies, new TestFunctionality()).execute(unitOfWork);

        assertThat(recorder.events).extracting(DynamicEvidenceEvent::getEventKind)
                .containsExactly("STEP_STARTED");
        assertThat(DynamicEvidenceContext.current()).isEmpty();

        stepFuture.complete(null);
        execution.join();

        assertThat(recorder.events).extracting(DynamicEvidenceEvent::getEventKind)
                .containsExactly("STEP_STARTED", "STEP_FINISHED");
        assertThat(recorder.events.stream()
                .filter(event -> "STEP_FINISHED".equals(event.getEventKind()))
                .count()).isEqualTo(1);
        assertThat(contextDuringStep.get().functionalityName()).isEqualTo("checkout");
        assertThat(contextDuringStep.get().stepName()).isEqualTo("reserveStock");
        assertThat(DynamicEvidenceContext.current()).isEmpty();
    }

    @Test
    void executeUntilStepEmitsStepEventsAndExposesContextDuringStepBody() {
        RecordingRecorder recorder = new RecordingRecorder();
        DynamicEvidenceRecorderHolder.setRecorder(recorder);
        TestUnitOfWork unitOfWork = new TestUnitOfWork(46L, "checkout");
        AtomicReference<DynamicEvidenceContext.StepContext> firstContext = new AtomicReference<>();
        AtomicReference<DynamicEvidenceContext.StepContext> secondContext = new AtomicReference<>();
        FlowStep reserveStock = new TestStep("reserveStock", () -> {
            firstContext.set(DynamicEvidenceContext.current().orElseThrow());
            assertThat(DynamicEvidenceContext.currentStepName()).contains("reserveStock");
        });
        FlowStep chargePayment = new TestStep("chargePayment", () -> {
            secondContext.set(DynamicEvidenceContext.current().orElseThrow());
            assertThat(DynamicEvidenceContext.currentFunctionalityName()).contains("checkout");
            assertThat(DynamicEvidenceContext.currentStepName()).contains("chargePayment");
        });
        chargePayment.setDependencies(new ArrayList<>(List.of(reserveStock)));
        ArrayList<FlowStep> plan = new ArrayList<>(List.of(reserveStock, chargePayment));
        HashMap<FlowStep, ArrayList<FlowStep>> dependencies = new HashMap<>();
        dependencies.put(reserveStock, new ArrayList<>());
        dependencies.put(chargePayment, new ArrayList<>(List.of(reserveStock)));

        new ExecutionPlan(plan, dependencies, new TestFunctionality())
                .executeUntilStep(chargePayment, unitOfWork)
                .join();

        assertThat(recorder.events).extracting(DynamicEvidenceEvent::getEventKind)
                .containsExactly("STEP_STARTED", "STEP_FINISHED", "STEP_STARTED", "STEP_FINISHED");
        assertThat(recorder.events).extracting(DynamicEvidenceEvent::getStepName)
                .containsExactly("reserveStock", "reserveStock", "chargePayment", "chargePayment");
        assertThat(recorder.events).allSatisfy(event -> {
            assertThat(event.getFunctionalityName()).isEqualTo("checkout");
            assertThat(event.getUnitOfWorkVersion()).isEqualTo(46L);
        });
        assertThat(firstContext.get().stepName()).isEqualTo("reserveStock");
        assertThat(firstContext.get().unitOfWorkVersion()).isEqualTo(46L);
        assertThat(secondContext.get().stepName()).isEqualTo("chargePayment");
        assertThat(secondContext.get().unitOfWorkVersion()).isEqualTo(46L);
        assertThat(DynamicEvidenceContext.current()).isEmpty();
    }

    @Test
    void interruptionDuringDelayEndsStepSpanOnceAndPreservesInterruptFlag() {
        Span stepSpan = mock(Span.class);
        setupTraceManagerStepSpan("checkout", "checkout::0", "reserveStock", stepSpan);

        RecordingRecorder recorder = new RecordingRecorder();
        DynamicEvidenceRecorderHolder.setRecorder(recorder);
        TestUnitOfWork unitOfWork = new TestUnitOfWork(44L, "checkout");
        FlowStep reserveStock = new TestStep("reserveStock", () -> {
        });
        ArrayList<FlowStep> plan = new ArrayList<>(List.of(reserveStock));
        HashMap<FlowStep, ArrayList<FlowStep>> dependencies = new HashMap<>();
        dependencies.put(reserveStock, new ArrayList<>());
        ExecutionPlan executionPlan = new ExecutionPlan(plan, dependencies, new TestFunctionality());
        setBehaviour(executionPlan, Map.of("reserveStock", List.of(0, 1, 0)));

        Thread.currentThread().interrupt();
        CompletableFuture<Void> execution = executionPlan.execute(unitOfWork);

        assertThatThrownBy(execution::join)
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(InterruptedException.class);
        assertThat(Thread.currentThread().isInterrupted()).isTrue();
        assertThat(recorder.events).extracting(DynamicEvidenceEvent::getEventKind)
                .containsExactly("STEP_STARTED", "STEP_FINISHED");
        verify(stepSpan, times(1)).end();
    }

    @Test
    void interruptionDuringAfterDelayEndsStepSpanOnceAndPreservesInterruptFlag() {
        Span stepSpan = mock(Span.class);
        setupTraceManagerStepSpan("checkout", "checkout::0", "reserveStock", stepSpan);

        RecordingRecorder recorder = new RecordingRecorder();
        DynamicEvidenceRecorderHolder.setRecorder(recorder);
        TestUnitOfWork unitOfWork = new TestUnitOfWork(45L, "checkout");
        FlowStep reserveStock = new TestStep("reserveStock", () -> {
            assertThat(DynamicEvidenceContext.currentStepName()).contains("reserveStock");
            Thread.currentThread().interrupt();
        });
        ArrayList<FlowStep> plan = new ArrayList<>(List.of(reserveStock));
        HashMap<FlowStep, ArrayList<FlowStep>> dependencies = new HashMap<>();
        dependencies.put(reserveStock, new ArrayList<>());
        ExecutionPlan executionPlan = new ExecutionPlan(plan, dependencies, new TestFunctionality());
        setBehaviour(executionPlan, Map.of("reserveStock", List.of(0, 0, 1)));

        CompletableFuture<Void> execution = executionPlan.execute(unitOfWork);

        assertThatThrownBy(execution::join)
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(InterruptedException.class);
        assertThat(Thread.currentThread().isInterrupted()).isTrue();
        assertThat(recorder.events).extracting(DynamicEvidenceEvent::getEventKind)
                .containsExactly("STEP_STARTED", "STEP_FINISHED");
        DynamicEvidenceEvent finished = recorder.events.get(1);
        assertThat(finished.getPayload()).containsEntry("outcome", "ERROR");
        assertThat(finished.getPayload()).containsEntry("errorType", InterruptedException.class.getName());
        assertThat(finished.getPayload()).containsKey("durationMillis");
        verify(stepSpan, times(1)).end();
    }

    private void setupTraceManagerStepSpan(String functionalityName, String invocationKey, String stepName, Span stepSpan) {
        TraceManager traceManager = TraceManager.getInstance();
        if (traceManager == null) {
            TraceManager.init("execution-plan-dynamic-evidence-test");
            traceManager = TraceManager.getInstance();
        }
        clearTraceManagerState();
        Map<String, ConcurrentLinkedQueue<String>> activeInvocationKeys = getField(traceManager, "activeInvocationKeys");
        Map<String, Span> stepSpans = getField(traceManager, "stepSpans");
        Map<String, Span> functionalitySpans = getField(traceManager, "functionalitySpans");

        activeInvocationKeys.put(functionalityName, new ConcurrentLinkedQueue<>(List.of(invocationKey)));
        functionalitySpans.remove(invocationKey);
        stepSpans.put(invocationKey + "::" + stepName, stepSpan);
    }

    private void clearTraceManagerState() {
        TraceManager traceManager = TraceManager.getInstance();
        if (traceManager == null) {
            return;
        }
        Map<String, ConcurrentLinkedQueue<String>> activeInvocationKeys = getField(traceManager, "activeInvocationKeys");
        Map<String, Span> stepSpans = getField(traceManager, "stepSpans");
        Map<String, Span> functionalitySpans = getField(traceManager, "functionalitySpans");
        activeInvocationKeys.clear();
        stepSpans.clear();
        functionalitySpans.clear();
    }

    private void setBehaviour(ExecutionPlan executionPlan, Map<String, List<Integer>> behaviour) {
        try {
            Field behaviourField = ExecutionPlan.class.getDeclaredField("behaviour");
            behaviourField.setAccessible(true);
            behaviourField.set(executionPlan, behaviour);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to set behaviour map for test", e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T getField(Object target, String fieldName) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(target);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to access field: " + fieldName, e);
        }
    }

    private static class RecordingRecorder implements DynamicEvidenceRecorder {
        private final List<DynamicEvidenceEvent> events = new CopyOnWriteArrayList<>();

        @Override
        public void record(DynamicEvidenceEvent event) {
            events.add(event);
        }

        @Override
        public void close() {
        }
    }

    private static class ThrowingRecorder implements DynamicEvidenceRecorder {
        private final List<String> attemptedEventKinds = new CopyOnWriteArrayList<>();
        private final Set<String> throwingEventKinds;

        ThrowingRecorder(Set<String> throwingEventKinds) {
            this.throwingEventKinds = throwingEventKinds;
        }

        @Override
        public void record(DynamicEvidenceEvent event) {
            attemptedEventKinds.add(event.getEventKind());
            if (throwingEventKinds.contains(event.getEventKind())) {
                throw new IllegalStateException("recorder failed on " + event.getEventKind());
            }
        }

        @Override
        public void close() {
        }
    }

    private static class TestUnitOfWork extends UnitOfWork {
        TestUnitOfWork(Long version, String functionalityName) {
            super(version, functionalityName);
        }
    }

    private static class TestFunctionality extends WorkflowFunctionality {
    }

    private static class TestStep extends FlowStep {
        private final Runnable body;

        TestStep(String stepName, Runnable body) {
            super(stepName);
            this.body = body;
        }

        @Override
        public CompletableFuture<Void> execute(UnitOfWork unitOfWork) {
            body.run();
            return CompletableFuture.completedFuture(null);
        }
    }

    private static class AsyncTestStep extends FlowStep {
        private final java.util.concurrent.Callable<CompletableFuture<Void>> body;

        AsyncTestStep(String stepName, java.util.concurrent.Callable<CompletableFuture<Void>> body) {
            super(stepName);
            this.body = body;
        }

        @Override
        public CompletableFuture<Void> execute(UnitOfWork unitOfWork) {
            try {
                return body.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}

package pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowRecoveryCheckpoint;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowStepRecoveryException;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowStepRecoveryResult;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.TraceManager;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate.SagaState;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SagaStepwiseRecoveryTest {

    private enum State implements SagaState {
        NORMAL, READ;
        public String getStateName() { return name(); }
    }

    private SagaUnitOfWorkService restoringService(AtomicReference<SagaState> state) {
        return new SagaUnitOfWorkService() {
            @Override
            public void sendAbortCommandsForStep(SagaUnitOfWork work, String stepName) {
                work.getPreviousStates().getOrDefault(stepName, List.of())
                        .forEach(record -> state.set(record.state()));
            }
        };
    }

    @Test
    void recoveryScopeTravelsWithSerializedCommandsWithoutDisablingLaterForwardRecording() {
        SagaUnitOfWork work = new SagaUnitOfWork(0L, "SERIALIZED_RECOVERY");
        work.getExecutedSteps().add("read");
        work.setCurrentExecutingStep("read");
        ObjectMapper mapper = new MessagingObjectMapperProvider(new ObjectMapper()).newMapper();
        work.registerCompensation("read", () -> {
            try {
                SagaUnitOfWork transported = mapper.readValue(
                        mapper.writeValueAsString(work), SagaUnitOfWork.class);
                transported.savePreviousState(7, GenericSagaState.NOT_IN_SAGA);
                assertThat(transported.getPreviousStates()).isEmpty();
            } catch (Exception failure) {
                throw new IllegalStateException(failure);
            }
        });

        new SagaUnitOfWorkService().recoverStepForExecutor(work, "read");

        work.setCurrentExecutingStep("later");
        work.savePreviousState(7, GenericSagaState.NOT_IN_SAGA);
        assertThat(work.getPreviousStates()).containsOnlyKeys("later");
    }

    @ParameterizedTest
    @CsvSource({"true,true", "true,false", "false,true", "false,false"})
    void compensationPreservesForwardUndoHistoryAndOriginalState(boolean executor, boolean failedLaterStep) {
        SagaUnitOfWork work = new SagaUnitOfWork(0L, "SEMANTIC_RECOVERY");
        AtomicReference<SagaState> state = new AtomicReference<>(State.NORMAL);
        work.getExecutedSteps().add("read");
        work.setCurrentExecutingStep("read");
        work.savePreviousState(7, state.get());
        state.set(State.READ);
        work.registerCompensation("read", () -> {
            // registerSagaState performs this capture before persisting its state change.
            work.savePreviousState(7, state.get());
            state.set(State.NORMAL);
        });
        if (failedLaterStep) {
            work.getExecutedSteps().add("leave");
            work.setCurrentExecutingStep("leave");
        }
        SagaUnitOfWorkService service = restoringService(state);

        if (executor) service.recoverStepForExecutor(work, "read");
        else service.abort(work);

        assertThat(state.get()).isEqualTo(State.NORMAL);
        assertThat(work.getPreviousStates()).containsOnlyKeys("read");
        assertThat(work.getPreviousStates().get("read")).containsExactly(
                new SagaUnitOfWork.AggregateStateRecord(7, State.NORMAL));
        assertThat(service.recoveryCheckpointsForExecutor(work)).isEmpty();
        assertThat(work.getCurrentExecutingStep()).isEqualTo(failedLaterStep ? "leave" : "read");
    }

    @Test
    void failedStateChangingCompensationStaysRetryableAndRestoresForwardRecording() {
        SagaUnitOfWork work = new SagaUnitOfWork(0L, "STATE_RECOVERY_RETRY");
        AtomicReference<SagaState> state = new AtomicReference<>(State.NORMAL);
        AtomicInteger attempts = new AtomicInteger();
        work.getExecutedSteps().addAll(List.of("read", "leave"));
        work.setCurrentExecutingStep("read");
        work.savePreviousState(7, state.get());
        state.set(State.READ);
        work.setCurrentExecutingStep("leave");
        work.registerCompensation("read", () -> {
            work.savePreviousState(7, state.get());
            state.set(State.NORMAL);
            if (attempts.incrementAndGet() == 1) throw new IllegalStateException("after state write");
        });
        SagaUnitOfWorkService service = restoringService(state);

        assertThatThrownBy(() -> service.recoverStepForExecutor(work, "read"))
                .isInstanceOf(WorkflowStepRecoveryException.class);
        assertThat(work.hasPendingCompensation("read")).isTrue();
        assertThat(work.getPreviousStates()).containsOnlyKeys("read");
        work.setCurrentExecutingStep("later-forward");
        work.savePreviousState(9, State.NORMAL);
        assertThat(work.getPreviousStates().get("later-forward")).hasSize(1);

        service.recoverStepForExecutor(work, "read");

        assertThat(attempts).hasValue(2);
        assertThat(state.get()).isEqualTo(State.NORMAL);
        assertThat(work.hasPendingCompensation("read")).isFalse();
        assertThat(work.getPreviousStates().get("read")).hasSize(1);
        assertThat(work.getPreviousStates().get("later-forward")).hasSize(1);
    }

    @BeforeAll
    static void initializeTracing() {
        TraceManager.init("saga-stepwise-recovery-test");
        TraceManager.getInstance().startRootSpan();
    }

    @Test
    void advancesExactlyOneRequestedCheckpointWithExplicitBeforeImplicitRecovery() {
        List<String> calls = new ArrayList<>();
        SagaUnitOfWork unitOfWork = new SagaUnitOfWork(0L, "STEPWISE");
        unitOfWork.getExecutedSteps().addAll(List.of("first", "second"));
        unitOfWork.registerCompensation("first", () -> calls.add("explicit-first"));
        unitOfWork.registerCompensation("second", () -> calls.add("explicit-second"));
        unitOfWork.setCurrentExecutingStep("second");
        unitOfWork.savePreviousState(7, GenericSagaState.NOT_IN_SAGA);
        SagaUnitOfWorkService service = new SagaUnitOfWorkService() {
            @Override
            public void sendAbortCommandsForStep(SagaUnitOfWork ignored, String stepName) {
                calls.add("implicit-" + stepName);
            }
        };

        WorkflowStepRecoveryResult second = service.recoverStepForExecutor(unitOfWork, "second");

        assertThat(second.sourceStepName()).isEqualTo("second");
        assertThat(second.explicitCompensationExecuted()).isTrue();
        assertThat(second.implicitRollbackExecuted()).isTrue();
        assertThat(calls).containsExactly("explicit-second", "implicit-second");
        assertThat(unitOfWork.isCompensationExecuted("first")).isFalse();
        assertThat(unitOfWork.isStepAborted("first")).isFalse();

        WorkflowStepRecoveryResult first = service.recoverStepForExecutor(unitOfWork, "first");

        assertThat(first.explicitCompensationExecuted()).isTrue();
        assertThat(first.implicitRollbackExecuted()).isFalse();
        assertThat(calls).containsExactly("explicit-second", "implicit-second", "explicit-first");
    }

    @Test
    void reportsPendingRuntimeRecoveryFromUnitOfWorkTruthInReverseExecutionOrder() {
        SagaUnitOfWork unitOfWork = new SagaUnitOfWork(0L, "RUNTIME_TRUTH");
        unitOfWork.getExecutedSteps().addAll(List.of("first", "partially-failed"));
        unitOfWork.registerCompensation("first", () -> { });
        unitOfWork.setCurrentExecutingStep("partially-failed");
        unitOfWork.savePreviousState(7, GenericSagaState.NOT_IN_SAGA);
        SagaUnitOfWorkService service = new SagaUnitOfWorkService();

        List<WorkflowRecoveryCheckpoint> checkpoints = service.recoveryCheckpointsForExecutor(unitOfWork);

        assertThat(checkpoints).containsExactly(
                new WorkflowRecoveryCheckpoint("partially-failed", false, true),
                new WorkflowRecoveryCheckpoint("first", true, false));
    }

    @Test
    void failedExplicitCompensationRemainsRetryableAndIsNotMarkedExecuted() {
        SagaUnitOfWork unitOfWork = new SagaUnitOfWork(0L, "RETRYABLE");
        unitOfWork.getExecutedSteps().add("step");
        AtomicInteger attempts = new AtomicInteger();
        unitOfWork.registerCompensation("step", () -> {
            if (attempts.incrementAndGet() == 1) {
                throw new IllegalStateException("first attempt fails");
            }
        });
        SagaUnitOfWorkService service = new SagaUnitOfWorkService();

        assertThatThrownBy(() -> service.recoverStepForExecutor(unitOfWork, "step"))
                .isInstanceOf(WorkflowStepRecoveryException.class)
                .hasCauseInstanceOf(IllegalStateException.class)
                .cause()
                .hasMessage("first attempt fails");
        assertThat(unitOfWork.isCompensationExecuted("step")).isFalse();
        assertThat(unitOfWork.isStepAborted("step")).isFalse();

        WorkflowStepRecoveryResult retry = service.recoverStepForExecutor(unitOfWork, "step");

        assertThat(retry.explicitCompensationExecuted()).isTrue();
        assertThat(attempts).hasValue(2);
        assertThat(unitOfWork.isCompensationExecuted("step")).isTrue();
    }

    @Test
    void implicitFailureReportsCompletedExplicitWorkAndLeavesRollbackRetryable() {
        List<String> calls = new ArrayList<>();
        SagaUnitOfWork unitOfWork = new SagaUnitOfWork(0L, "IMPLICIT_RETRYABLE");
        unitOfWork.getExecutedSteps().add("step");
        unitOfWork.registerCompensation("step", () -> calls.add("explicit"));
        unitOfWork.setCurrentExecutingStep("step");
        unitOfWork.savePreviousState(7, GenericSagaState.NOT_IN_SAGA);
        AtomicInteger implicitAttempts = new AtomicInteger();
        SagaUnitOfWorkService service = new SagaUnitOfWorkService() {
            @Override
            public void sendAbortCommandsForStep(SagaUnitOfWork ignored, String stepName) {
                calls.add("implicit");
                if (implicitAttempts.incrementAndGet() == 1) {
                    throw new IllegalStateException("implicit failed");
                }
            }
        };

        assertThatThrownBy(() -> service.recoverStepForExecutor(unitOfWork, "step"))
                .isInstanceOfSatisfying(WorkflowStepRecoveryException.class, failure -> {
                    assertThat(failure.failedRecoveryKind()).isEqualTo("IMPLICIT_SAGA_ROLLBACK");
                    assertThat(failure.completedRecovery().explicitCompensationExecuted()).isTrue();
                    assertThat(failure.completedRecovery().implicitRollbackExecuted()).isFalse();
                    assertThat(failure.getCause()).hasMessage("implicit failed");
                });
        assertThat(unitOfWork.isCompensationExecuted("step")).isTrue();
        assertThat(unitOfWork.isStepAborted("step")).isFalse();

        WorkflowStepRecoveryResult retry = service.recoverStepForExecutor(unitOfWork, "step");

        assertThat(retry.explicitCompensationExecuted()).isFalse();
        assertThat(retry.implicitRollbackExecuted()).isTrue();
        assertThat(unitOfWork.isStepAborted("step")).isTrue();
        assertThat(calls).containsExactly("explicit", "implicit", "implicit");
    }
}

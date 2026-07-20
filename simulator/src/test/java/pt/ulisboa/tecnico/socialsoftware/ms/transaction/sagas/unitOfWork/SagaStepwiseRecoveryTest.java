package pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowStepRecoveryResult;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.TraceManager;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SagaStepwiseRecoveryTest {

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
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("first attempt fails");
        assertThat(unitOfWork.isCompensationExecuted("step")).isFalse();
        assertThat(unitOfWork.isStepAborted("step")).isFalse();

        WorkflowStepRecoveryResult retry = service.recoverStepForExecutor(unitOfWork, "step");

        assertThat(retry.explicitCompensationExecuted()).isTrue();
        assertThat(attempts).hasValue(2);
        assertThat(unitOfWork.isCompensationExecuted("step")).isTrue();
    }
}

package pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFinalizationResult;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.TraceManager;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class SagaExecutorControlTest {

    @BeforeAll
    static void initializeTracing() {
        TraceManager.init("saga-executor-control-test");
        TraceManager.getInstance().startRootSpan();
    }

    @Test
    void assignedPreBodyAbortDoesNotRunOrRecordTargetAndLeavesPriorCheckpointRecoverable() {
        SagaUnitOfWorkService service = spy(new SagaUnitOfWorkService());
        SagaUnitOfWork unitOfWork = new SagaUnitOfWork(0L, "PRE_BODY_ABORT");
        List<String> calls = new ArrayList<>();
        TestFunctionality functionality = new TestFunctionality(service, unitOfWork, calls);

        functionality.executeUntilStep("first", unitOfWork);
        functionality.abortBeforeStepForExecutor("second", unitOfWork);

        assertThat(calls).containsExactly("first");
        assertThat(unitOfWork.getExecutedSteps()).containsExactly("first");
        assertThat(unitOfWork.getExecutedSteps()).doesNotContain("second");
        assertThat(functionality.recoverStepForExecutor("first", unitOfWork).explicitCompensationExecuted()).isTrue();
        assertThat(calls).containsExactly("first", "compensate-first");
        verify(service, never()).abort(unitOfWork);
    }

    @Test
    void exactExecutorStepRunsOnlyTheNamedReadyBranchAndLeavesLaterAssignedTargetUntouched() {
        SagaUnitOfWorkService service = spy(new SagaUnitOfWorkService());
        SagaUnitOfWork unitOfWork = new SagaUnitOfWork(0L, "EXACT_BRANCH");
        List<String> calls = new ArrayList<>();
        BranchedFunctionality functionality = new BranchedFunctionality(service, unitOfWork, calls);

        functionality.executeStepForExecutor("first", unitOfWork);
        functionality.executeStepForExecutor("second", unitOfWork);
        functionality.abortBeforeStepForExecutor("third", unitOfWork);

        assertThat(calls).containsExactly("first", "second");
        assertThat(unitOfWork.getExecutedSteps()).containsExactly("first", "second");
        assertThat(unitOfWork.getExecutedSteps()).doesNotContain("third");
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> functionality.executeStepForExecutor("second", unitOfWork))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already attempted");
        assertThat(calls).containsExactly("first", "second");
    }

    @Test
    void exactExecutorStepRejectsUnmetDependenciesWithoutRunningAnyBody() {
        SagaUnitOfWorkService service = spy(new SagaUnitOfWorkService());
        SagaUnitOfWork unitOfWork = new SagaUnitOfWork(0L, "INVALID_BRANCH");
        List<String> calls = new ArrayList<>();
        BranchedFunctionality functionality = new BranchedFunctionality(service, unitOfWork, calls);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> functionality.executeStepForExecutor("second", unitOfWork))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unmet dependencies");

        assertThat(calls).isEmpty();
        assertThat(unitOfWork.getExecutedSteps()).isEmpty();
    }

    @Test
    void controlledFinalizationCommitsWithoutRunningOpaqueResumeRecovery() {
        SagaUnitOfWorkService service = spy(new SagaUnitOfWorkService());
        SagaUnitOfWork unitOfWork = new SagaUnitOfWork(0L, "FINALIZE");
        TestFunctionality functionality = new TestFunctionality(service, unitOfWork, new ArrayList<>());

        functionality.executeUntilStep("second", unitOfWork);
        WorkflowFinalizationResult result = functionality.finalizeForExecutor(unitOfWork);

        assertThat(result.committed()).isTrue();
        assertThat(result.failure()).isNull();
        verify(service).commit(unitOfWork);
        verify(service, never()).abort(unitOfWork);
    }

    @Test
    void controlledCommitFailureIsExposedWithoutConsumingRecoveryState() {
        SagaUnitOfWorkService service = spy(new SagaUnitOfWorkService());
        SagaUnitOfWork unitOfWork = new SagaUnitOfWork(0L, "FAILED_FINALIZE");
        List<String> calls = new ArrayList<>();
        TestFunctionality functionality = new TestFunctionality(service, unitOfWork, calls);
        IllegalStateException commitFailure = new IllegalStateException("commit failed");
        doThrow(commitFailure).when(service).commit(unitOfWork);

        functionality.executeUntilStep("second", unitOfWork);
        WorkflowFinalizationResult result = functionality.finalizeForExecutor(unitOfWork);

        assertThat(result.committed()).isFalse();
        assertThat(result.failure()).isSameAs(commitFailure);
        assertThat(unitOfWork.getExecutedSteps()).containsExactly("first", "second");
        assertThat(unitOfWork.isCompensationExecuted("first")).isFalse();
        assertThat(unitOfWork.isCompensationExecuted("second")).isFalse();
        verify(service, never()).abort(unitOfWork);

        assertThat(functionality.recoverStepForExecutor("second", unitOfWork).explicitCompensationExecuted()).isTrue();
        assertThat(calls).contains("compensate-second");
    }

    private static final class BranchedFunctionality extends WorkflowFunctionality {
        private BranchedFunctionality(SagaUnitOfWorkService service, SagaUnitOfWork unitOfWork, List<String> calls) {
            SagaWorkflow sagaWorkflow = new SagaWorkflow(this, service, unitOfWork);
            SagaStep first = new SagaStep("first", () -> calls.add("first"));
            SagaStep second = new SagaStep("second", () -> calls.add("second"), new ArrayList<>(List.of(first)));
            SagaStep third = new SagaStep("third", () -> calls.add("third"), new ArrayList<>(List.of(first)));
            sagaWorkflow.addStep(first);
            sagaWorkflow.addStep(second);
            sagaWorkflow.addStep(third);
            this.workflow = sagaWorkflow;
        }
    }

    private static final class TestFunctionality extends WorkflowFunctionality {
        private TestFunctionality(SagaUnitOfWorkService service, SagaUnitOfWork unitOfWork, List<String> calls) {
            SagaWorkflow sagaWorkflow = new SagaWorkflow(this, service, unitOfWork);
            SagaStep first = new SagaStep("first", () -> calls.add("first"));
            first.registerCompensation(() -> calls.add("compensate-first"), unitOfWork);
            SagaStep second = new SagaStep("second", () -> calls.add("second"), new ArrayList<>(List.of(first)));
            second.registerCompensation(() -> calls.add("compensate-second"), unitOfWork);
            sagaWorkflow.addStep(first);
            sagaWorkflow.addStep(second);
            this.workflow = sagaWorkflow;
        }
    }
}

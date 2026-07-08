package pt.ulisboa.tecnico.socialsoftware.ms.faults;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.ExecutionPlan;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.FlowStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;
import pt.ulisboa.tecnico.socialsoftware.ms.impairment.ImpairmentHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.TraceManager;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class FaultVectorProviderTest {
    @TempDir
    Path tempDir;

    @AfterEach
    void cleanup() {
        FaultVectorProviderHolder.clear();
        ImpairmentHandler.setDirectory(null);
        ImpairmentHandler.getInstance().cleanUpCounter();
        Thread.interrupted();
    }

    @Test
    void scopedProviderClearsStateAndRejectsConcurrentProviders() {
        FaultVectorFaultProvider provider = context -> Optional.empty();

        FaultVectorProviderHolder.Scope scope = FaultVectorProviderHolder.install(provider);

        assertThat(FaultVectorProviderHolder.isActive()).isTrue();
        assertThatThrownBy(() -> FaultVectorProviderHolder.install(context -> Optional.empty()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already active");

        scope.close();

        assertThat(FaultVectorProviderHolder.isActive()).isFalse();
        assertThatCode(() -> FaultVectorProviderHolder.install(context -> Optional.empty()).close())
                .doesNotThrowAnyException();
    }

    @Test
    void boundaryScopeClearsCurrentContextOnClose() {
        FaultVectorBoundaryContext context = context("scheduled-step-1", 0, "reserveStock", 1);

        try (FaultVectorProviderHolder.BoundaryScope ignored = FaultVectorProviderHolder.enterBoundary(context)) {
            assertThat(FaultVectorProviderHolder.currentBoundary()).contains(context);
        }

        assertThat(FaultVectorProviderHolder.currentBoundary()).isEmpty();
    }

    @Test
    void activeProviderInjectsTypedFaultBeforeStepBodyAndCarriesIdentity() {
        AtomicBoolean bodyExecuted = new AtomicBoolean(false);
        FlowStep reserveStock = new TestStep("reserveStock", () -> bodyExecuted.set(true));
        ExecutionPlan executionPlan = executionPlan(List.of(reserveStock));
        FaultVectorBoundaryContext context = context("scheduled-step-1", 0, "reserveStock", 1);
        FaultVectorFault fault = FaultVectorFault.from(context);

        try (FaultVectorProviderHolder.Scope ignoredProvider = FaultVectorProviderHolder.install(new InMemoryFaultVectorProvider(Map.of(0, fault)));
             FaultVectorProviderHolder.BoundaryScope ignoredBoundary = FaultVectorProviderHolder.enterBoundary(context)) {
            assertThatThrownBy(() -> executionPlan.execute(new TestUnitOfWork(1L, "checkout")).join())
                    .isInstanceOf(CompletionException.class)
                    .hasCauseInstanceOf(FaultVectorInjectedFaultException.class)
                    .satisfies(error -> {
                        FaultVectorInjectedFaultException injected = (FaultVectorInjectedFaultException) error.getCause();
                        assertThat(injected.getScenarioExecutionId()).isEqualTo("scenario-execution-1");
                        assertThat(injected.getScenarioPlanId()).isEqualTo("scenario-plan-1");
                        assertThat(injected.getSagaInstanceId()).isEqualTo("saga-1");
                        assertThat(injected.getScheduledStepId()).isEqualTo("scheduled-step-1");
                        assertThat(injected.getSlotIndex()).isEqualTo(0);
                        assertThat(injected.getRuntimeStepName()).isEqualTo("reserveStock");
                    });
        }

        assertThat(bodyExecuted).isFalse();
    }

    @Test
    void executeUntilStepFaultsTargetSlotWithoutPreemptingPrefixStepBody() {
        AtomicBoolean firstBodyExecuted = new AtomicBoolean(false);
        AtomicBoolean secondBodyExecuted = new AtomicBoolean(false);
        FlowStep reserveStock = new TestStep("reserveStock", () -> firstBodyExecuted.set(true));
        FlowStep chargePayment = new TestStep("chargePayment", () -> secondBodyExecuted.set(true));
        chargePayment.setDependencies(new ArrayList<>(List.of(reserveStock)));
        ExecutionPlan executionPlan = executionPlan(List.of(reserveStock, chargePayment));
        FaultVectorBoundaryContext context = context("scheduled-step-2", 1, "chargePayment", 1);
        FaultVectorFault fault = FaultVectorFault.from(context);

        try (FaultVectorProviderHolder.Scope ignoredProvider = FaultVectorProviderHolder.install(new InMemoryFaultVectorProvider(Map.of(1, fault)));
             FaultVectorProviderHolder.BoundaryScope ignoredBoundary = FaultVectorProviderHolder.enterBoundary(context)) {
            assertThatThrownBy(() -> executionPlan.executeUntilStep(chargePayment, new TestUnitOfWork(1L, "checkout")).join())
                    .isInstanceOf(CompletionException.class)
                    .hasCauseInstanceOf(FaultVectorInjectedFaultException.class);
        }

        assertThat(firstBodyExecuted).isTrue();
        assertThat(secondBodyExecuted).isFalse();
    }

    @Test
    void legacyCsvFaultStillAppliesWhenNoProviderIsActive() throws Exception {
        writeCsvFault("TestFunctionality", "reserveStock");
        ImpairmentHandler.setDirectory(tempDir.toString());
        FlowStep reserveStock = new TestStep("reserveStock", () -> { });
        ExecutionPlan executionPlan = executionPlan(List.of(reserveStock));

        assertThatThrownBy(() -> executionPlan.execute(new TestUnitOfWork(1L, "checkout")))
                .isInstanceOf(SimulatorException.class)
                .isNotInstanceOf(FaultVectorInjectedFaultException.class)
                .hasMessageContaining("Fault on reserveStock");
    }

    @Test
    void activeProviderSuppressesCsvFaultsAndDelaysWhenItDoesNotInject() throws Exception {
        writeCsvFault("TestFunctionality", "reserveStock");
        ImpairmentHandler.setDirectory(tempDir.toString());
        AtomicBoolean bodyExecuted = new AtomicBoolean(false);

        try (FaultVectorProviderHolder.Scope ignoredProvider = FaultVectorProviderHolder.install(context -> Optional.empty())) {
            FlowStep reserveStock = new TestStep("reserveStock", () -> bodyExecuted.set(true));
            ExecutionPlan executionPlan = executionPlan(List.of(reserveStock));

            executionPlan.execute(new TestUnitOfWork(1L, "checkout")).join();

            assertThat(bodyExecuted).isTrue();
            assertThat(executionPlan.getTotalDelay()).isZero();
            assertThat(executionPlan.getBehaviour()).isEqualTo("{}");
        }
    }

    @Test
    void injectedFaultThroughWorkflowExecuteUntilStepMakesCompensationLegal() {
        TraceManager.init("fault-vector-provider-test");
        TraceManager.getInstance().startRootSpan();
        SagaUnitOfWorkService unitOfWorkService = mock(SagaUnitOfWorkService.class);
        WorkflowFunctionality functionality = new TestFunctionality();
        SagaUnitOfWork unitOfWork = new SagaUnitOfWork(1L, "checkout");
        SagaWorkflow workflow = new SagaWorkflow(functionality, unitOfWorkService, unitOfWork);
        SagaStep reserveStock = new SagaStep("reserveStock", () -> { });
        workflow.addStep(reserveStock);
        FaultVectorBoundaryContext context = context("scheduled-step-1", 0, "reserveStock", 1);
        FaultVectorFault fault = FaultVectorFault.from(context);

        try (FaultVectorProviderHolder.Scope ignoredProvider = FaultVectorProviderHolder.install(new InMemoryFaultVectorProvider(Map.of(0, fault)));
             FaultVectorProviderHolder.BoundaryScope ignoredBoundary = FaultVectorProviderHolder.enterBoundary(context)) {
            assertThatThrownBy(() -> workflow.executeUntilStep("reserveStock", unitOfWork))
                    .isInstanceOf(CompletionException.class)
                    .hasCauseInstanceOf(FaultVectorInjectedFaultException.class);

            assertThatCode(() -> workflow.resumeCompensation(unitOfWork)).doesNotThrowAnyException();
        } finally {
            TraceManager.getInstance().endRootSpan();
        }

        verify(unitOfWorkService).abort(unitOfWork);
    }

    private FaultVectorBoundaryContext context(String scheduledStepId, int slotIndex, String runtimeStepName, int assignedBit) {
        return new FaultVectorBoundaryContext(
                "scenario-execution-1",
                "scenario-plan-1",
                "saga-1",
                scheduledStepId,
                slotIndex,
                TestFunctionality.class.getName(),
                TestFunctionality.class.getSimpleName(),
                runtimeStepName,
                assignedBit);
    }

    private ExecutionPlan executionPlan(List<FlowStep> steps) {
        HashMap<FlowStep, ArrayList<FlowStep>> dependencies = new HashMap<>();
        for (FlowStep step : steps) {
            dependencies.put(step, step.getDependencies());
        }
        return new ExecutionPlan(new ArrayList<>(steps), dependencies, new TestFunctionality());
    }

    private void writeCsvFault(String functionalityName, String stepName) throws Exception {
        Files.writeString(tempDir.resolve(functionalityName + ".csv"), "run\n" + stepName + ",1,0,0\n");
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
}

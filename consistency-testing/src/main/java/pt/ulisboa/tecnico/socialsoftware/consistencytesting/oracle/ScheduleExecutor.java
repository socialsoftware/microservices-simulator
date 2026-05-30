package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.DeferredEventApplicationService.CaptureSession;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.utils.EventUtils;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventHandling;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;

class ScheduleExecutor {
    // TODO internal StepDependencyGraph could be more efficient and cleaner

    private static final int STEP_EXECUTION_LIMIT = 500;

    private static final Logger log = LoggerFactory.getLogger(ScheduleExecutor.class);

    private final SagaUnitOfWorkService uowService;
    private final CaptureSession captureSession;
    private final List<EventHandling> eventHandlings;
    private final Map<FunctionalityId, WorkflowFunctionality> functionalities;
    private final StepDependencies interDependencies;
    private final StepDependencies intraDependencies = new StepDependencies();
    private final Map<StepId, OracleStep> steps = new HashMap<>();
    private final Set<StepId> schedule = new LinkedHashSet<>(); // keeps execution order and allows O(1) contains checks
    private final Set<StepId> successfulSteps = new HashSet<>();
    private final Map<StepId, Exception> stepExceptionsMap = new HashMap<>();
    private final Set<TestStatus> detectedStatuses = new HashSet<>();

    public ScheduleExecutor(
            Map<FunctionalityId, WorkflowFunctionality> functionalities,
            StepDependencies interDependencies,
            SagaUnitOfWorkService uowService,
            DeferredEventApplicationService.CaptureSession captureSession,
            List<EventHandling> eventHandlings) {

        this.functionalities = Map.copyOf(functionalities);
        this.interDependencies = new StepDependencies(interDependencies);
        this.uowService = uowService;
        this.captureSession = captureSession;
        this.eventHandlings = eventHandlings;

        for (Entry<FunctionalityId, WorkflowFunctionality> funcEntry : functionalities.entrySet()) {
            addSteps(OracleStepFactory.buildStepsForFunctionality(
                    funcEntry.getKey(), funcEntry.getValue(), uowService));
        }
    }

    private void addSteps(Collection<? extends OracleStep> newSteps) {
        Set<StepId> seenIds = new HashSet<>();

        for (OracleStep step : newSteps) {
            StepId id = step.getId();

            // Check if step exists in the main map OR if it's duplicated in this batch
            if (steps.containsKey(id) || !seenIds.add(id)) {
                throw new IllegalArgumentException("Step '%s' already exists, can't be overridden".formatted(id));
            }
        }

        for (OracleStep newStep : newSteps) {
            steps.put(newStep.getId(), newStep);
        }

        intraDependencies.merge(StepDependencies.of(newSteps));
    }

    public TestResult execute() {
        executeSteps();
        evaluateTestCompletionStatus();

        return new TestResult(
                intraDependencies,
                interDependencies,
                functionalities,
                List.copyOf(schedule), // list will reflect the LinkedHashSet order
                stepExceptionsMap,
                detectedStatuses);
    }

    private void evaluateTestCompletionStatus() {
        int totalSteps = steps.size(); // TODO should this account for the interdeps?

        if (schedule.size() >= STEP_EXECUTION_LIMIT) {
            detectedStatuses.add(TestStatus.EXECUTION_LIMIT_EXCEEDED);
        } else if (schedule.size() < totalSteps) {
            // ! TODO review SCHEDULE_REJECTED and DEADLOCK statuses
            detectedStatuses.add(TestStatus.SCHEDULE_REJECTED);
        }
    }

    private void executeSteps() {
        while (schedule.size() < STEP_EXECUTION_LIMIT) {
            Optional<OracleStep> stepOpt = getNextStep();
            if (stepOpt.isEmpty()) {
                break;
            }

            OracleStep step = stepOpt.get();
            StepId stepId = step.getId();

            if (schedule.contains(stepId)) {
                throw new IllegalStateException("Step '%s' cannot be executed more than once".formatted(stepId));
            }

            schedule.add(stepId);
            try {
                step.execute();
                successfulSteps.add(stepId);
            } catch (Exception e) {
                boolean isCriticalFailure = handleStepFailure(step, e);
                if (isCriticalFailure) {
                    break; // defensive break to not continue to test on a broken system state
                }
            }

            captureEmittedEventSteps(stepId);
        }
    }

    private void captureEmittedEventSteps(StepId stepId) {
        // Run all event handling routines at once to capture events emitted by the step.
        EventUtils.runEventHandlingScheduledTasks(eventHandlings);
        Set<DeferredEventInvocation> eventInvocations = captureSession.drain();

        // TODO should capture all events, or filter to selected EventHandlers for test?
        List<EventHandlerStep> eventHandlerSteps = eventInvocations.stream()
                .map(invocation -> new EventHandlerStep(
                        invocation.event(),
                        invocation.handler(),
                        stepId,
                        invocation.publisherAggregateId(),
                        invocation.subscriberAggregateId()))
                .toList();

        addSteps(eventHandlerSteps);
    }

    /**
     * Handles the failure of a step during test execution.
     * 
     * @param step the failed step
     * @param e    the exception that caused the failure
     * @return {@code true} if the failure is critical and should stop the
     *         test execution, {@code false} if its safe to continue the test
     *         execution (e.g. business exception on functionality step)
     */
    private boolean handleStepFailure(OracleStep step, Exception e) {
        boolean isCriticalFailure = false;
        StepId stepId = step.getId();

        if (e instanceof CompletionException ce && ce.getCause() instanceof Exception cause) {
            // unwrap CompletionExceptions if they wrap Exception(excludes Throwables, null)
            e = cause;
        }

        stepExceptionsMap.put(stepId, e);

        if (!(e instanceof SimulatorException)) {
            log.error(
                    "Step '{}' failed with an unexpected system exception, which indicates a possible broken system state. Stopping test execution",
                    stepId, e);
            detectedStatuses.add(TestStatus.INTERNAL_EXCEPTION);
            isCriticalFailure = true;
        }

        if (step instanceof CommitStep || step instanceof AbortStep) {
            log.error(
                    "[{}] '{}' failed, resulting in a broken system state. Stopping test execution",
                    step.getClass().getName(), stepId, e);
            detectedStatuses.add(TestStatus.INTERNAL_EXCEPTION);
            isCriticalFailure = true;

        } else if (step instanceof CompensationStep || step instanceof EventHandlerStep) {
            log.warn("[{}] '{}' failed. Continuing test normally",
                    step.getClass().getName(), stepId, e);

        } else if (step instanceof FunctionalityStep funcStep) {
            log.info(
                    "[{}] '{}' failed with a domain exception. Injecting compensation path in test",
                    step.getClass().getName(), stepId);
            injectCompensation(funcStep, stepId);

        } else {
            throw new IllegalStateException("Unknown [%s] implementation type for step %s: [%s]"
                    .formatted(OracleStep.class.getName(), stepId, step.getClass().getName()));
        }

        return isCriticalFailure;
    }

    private void injectCompensation(FunctionalityStep funcStep, StepId stepId) {
        FunctionalityId funcId = funcStep.getFunctionalityId();
        WorkflowFunctionality func = functionalities.get(funcId);

        if (func == null) {
            throw new IllegalStateException(
                    "Functionality '%s' not found in registered test functionalities for step '%s'"
                            .formatted(funcId, stepId));
        }

        addSteps(OracleStepFactory.buildStepsForFunctionalityCompensation(funcId, func, uowService));
    }

    private Optional<OracleStep> getNextStep() {
        // TODO should be changed to pseudo-random pick (or determinisitic for testing)
        return steps.values().stream()
                .filter(step -> stepCanExecute(step.getId()))
                .findFirst();
    }

    private boolean stepCanExecute(StepId stepId) {
        return !schedule.contains(stepId) && stepDependenciesSatisfied(stepId);
    }

    private boolean stepDependenciesSatisfied(StepId stepId) {
        // intra-dependencies need to be successful to release
        boolean intraDepsSatisfied = successfulSteps.containsAll(intraDependencies.getStepDependencies(stepId));

        // inter-dependencies only need to have executed (successful or not) to release
        boolean interDepsSatisfied = schedule.containsAll(interDependencies.getStepDependencies(stepId));

        return intraDepsSatisfied && interDepsSatisfied;
    }
}

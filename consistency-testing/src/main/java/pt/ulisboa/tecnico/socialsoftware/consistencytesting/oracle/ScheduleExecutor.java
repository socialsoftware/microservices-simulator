package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.utils.EventUtils;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventHandling;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;

final class ScheduleExecutor {
    // TODO internal StepDependencyGraph could be more efficient and cleaner

    // TODO Should implement TestStatus.DEADLOCK verification.
    // * Happens when the depedency graph (intra + inter deps) has cycles.
    // * Should the verification be robust to steps/intra-deps that spawn at
    // * runtime, or just the ones known at initialization time?

    private static final int STEP_EXECUTION_LIMIT = 500;

    static final FunctionalityId INITIAL_STATE_SETUP_FUNCTIONALITY_ID = FunctionalityId
            .forSagaFunctionality("initialStateSetup");

    static final StepId INITIAL_STATE_SETUP_STEP_ID = StepId.forFunctionalityStep(
            INITIAL_STATE_SETUP_FUNCTIONALITY_ID, "step");

    private static final Logger log = LoggerFactory.getLogger(ScheduleExecutor.class);

    private final SagaUnitOfWorkService uowService;
    private final TracingSagaUnitOfWorkService.TraceSession traceSession;
    private final DeferredEventApplicationService.CaptureSession captureSession;
    private final Set<EventHandling> eventHandlings;
    private final Random scheduleRng;
    private final Map<FunctionalityId, WorkflowFunctionality> functionalities;
    private final Set<InterInvariant> interInvariants;
    private final StepDependencies interDependencies;
    private final StepDependencies intraDependencies = new StepDependencies();
    private final Map<StepId, OracleStep> steps = new HashMap<>();
    private final Set<StepId> schedule = new LinkedHashSet<>(); // keeps execution order and allows O(1) contains checks
    private final Set<StepId> successfulSteps = new HashSet<>();
    private final Map<StepId, Exception> stepExceptionsMap = new HashMap<>();

    /** inter-invariant name -> violations detected for that inter-invariant */
    private final Map<String, Set<InterInvariantViolation>> interInvariantViolations = new HashMap<>();
    private final Set<TestStatus> detectedStatuses = new HashSet<>();

    /** aggregateId -> the step that most recently wrote it */
    private final Map<Integer, StepId> lastWriterByAggregate = new HashMap<>();
    private final Set<ReadsFromRelation> readsFromRelations = new HashSet<>();

    ScheduleExecutor(
            Map<FunctionalityId, WorkflowFunctionality> functionalities,
            Set<InterInvariant> interInvariants,
            StepDependencies interDependencies,
            SagaUnitOfWorkService uowService,
            TracingSagaUnitOfWorkService.TraceSession traceSession,
            DeferredEventApplicationService.CaptureSession captureSession,
            Set<EventHandling> eventHandlings,
            long schedulerSeed) {

        this.functionalities = Map.copyOf(functionalities);
        this.interInvariants = Set.copyOf(interInvariants);
        this.interDependencies = new StepDependencies(interDependencies);
        this.uowService = uowService;
        this.captureSession = captureSession;
        this.traceSession = traceSession;
        this.eventHandlings = eventHandlings;
        this.scheduleRng = new Random(schedulerSeed);

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

    TestResult execute() {
        executeSteps();
        evaluateTestCompletionStatus();
        checkInterInvariants();

        return new TestResult(
                intraDependencies,
                interDependencies,
                functionalities,
                List.copyOf(schedule), // list will reflect the LinkedHashSet order
                stepExceptionsMap,
                detectedStatuses,
                readsFromRelations,
                interInvariantViolations);
    }

    private void evaluateTestCompletionStatus() {
        if (detectedStatuses.contains(TestStatus.INTERNAL_SYSTEM_EXCEPTION)
                || detectedStatuses.contains(TestStatus.CRITICAL_STEP_FAILURE)) {
            return; // when a critical failure is detected other statuses are invalidated
        }

        if (schedule.size() >= STEP_EXECUTION_LIMIT) {
            detectedStatuses.add(TestStatus.EXECUTION_LIMIT_EXCEEDED);
            return;
        }

        if (!interDependencies.getSteps().stream().allMatch(schedule::contains)) {
            // TODO should it be possible to specify which inter-dep(s) were impossible?
            detectedStatuses.add(TestStatus.INTERDEPENDENCY_RESOLUTION_FAILED);
        }
    }

    private void checkInterInvariants() {
        for (InterInvariant interInvariant : interInvariants) {
            Set<InterInvariantViolation> violations = interInvariant.predicate().get();

            if (violations.isEmpty()) {
                continue;
            }

            log.error("Inter-invariant '{}' violations: {}",
                    interInvariant.name(),
                    violations.stream().map(InterInvariantViolation::description).toList());

            detectedStatuses.add(TestStatus.INTER_INVARIANT_VIOLATION);

            interInvariantViolations.put(interInvariant.name(), violations);
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

            captureReadsFromRelations(stepId);
            captureEmittedEventSteps(stepId);
        }
    }

    private void captureReadsFromRelations(StepId stepId) {
        Set<Integer> writtenByThisStep = new HashSet<>();
        for (Effect effect : traceSession.drain()) {
            switch (effect) {
                case Effect.Write write -> {
                    lastWriterByAggregate.put(write.aggregateId(), stepId);
                    writtenByThisStep.add(write.aggregateId());
                }
                case Effect.Read read -> {
                    if (writtenByThisStep.contains(read.aggregateId())) {
                        continue; // reading what this step just wrote is not a cross-step reads-from
                    }
                    StepId writer = lastWriterByAggregate.getOrDefault(
                            read.aggregateId(), INITIAL_STATE_SETUP_STEP_ID);
                    readsFromRelations.add(new ReadsFromRelation(stepId, writer, read.aggregateType()));
                }
            }
        }
    }

    private void captureEmittedEventSteps(StepId stepId) {
        // Run all event handling routines at once to capture events emitted by step.
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
        if (e instanceof CompletionException ce && ce.getCause() instanceof Exception cause) {
            // unwrap CompletionExceptions if they wrap Exception(excludes Throwables, null)
            e = cause;
        }

        stepExceptionsMap.put(step.getId(), e);

        if (!(e instanceof SimulatorException)) {
            log.error(
                    "Step '{}' failed with an unexpected system exception, which indicates a possible broken system state. Stopping test execution",
                    step.getId(), e);
            detectedStatuses.add(TestStatus.INTERNAL_SYSTEM_EXCEPTION);
            return true;
        }

        return switch (step) {
            case FunctionalityStep funcStep -> logBenignStepFailureAndInjectCompensation(funcStep);

            // critical failures
            case CompensationStep compensationStep -> logCriticalStepFailureAndRegisterStatus(step, e);
            case CommitStep commitStep -> logCriticalStepFailureAndRegisterStatus(step, e);
            case AbortStep abortStep -> logCriticalStepFailureAndRegisterStatus(step, e);
            case EventHandlerStep eventHandlerStep -> logCriticalStepFailureAndRegisterStatus(step, e);
        };
    }

    /**
     * @return {@code true}, to indicate that it was a critical failure.
     */
    private boolean logCriticalStepFailureAndRegisterStatus(OracleStep step, Exception e) {
        log.error(
                "Critical step '{}' of type [{}] failed, resulting in a broken system state. Stopping test execution",
                step.getId(), step.getClass().getName(), e);
        detectedStatuses.add(TestStatus.CRITICAL_STEP_FAILURE);
        return true;
    }

    /**
     * @return {@code false}, to indicate that it was not a critical failure.
     */
    private boolean logBenignStepFailureAndInjectCompensation(FunctionalityStep funcStep) {
        log.info(
                "[{}] '{}' failed with a domain exception. Injecting compensation path for functionality '{}' in the test",
                funcStep.getClass().getName(), funcStep.getId(), funcStep.getFunctionalityId());
        injectCompensation(funcStep, funcStep.getId());
        return false;
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
        // Uniform random pick over the ReadySet, seeded for reproducibility.
        // Candidates are ordered by their stable StepId first, so the pick does not
        // depend on HashMap iteration order
        // (which is not guaranteed stable across JVMs).
        List<OracleStep> readySet = steps.values().stream()
                .filter(step -> stepCanExecute(step.getId()))
                .sorted(Comparator.comparing(step -> step.getId().toString()))
                .toList();

        if (readySet.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(readySet.get(scheduleRng.nextInt(readySet.size())));
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

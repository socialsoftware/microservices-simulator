package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.ArrayList;
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

    private static final int STEP_EXECUTION_LIMIT = 100;

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
    private final DeferredEventRetryTracker deferredEventRetryTracker = new DeferredEventRetryTracker();

    /** inter-invariant name -> violations detected for that inter-invariant */
    private final Map<String, Set<InterInvariantViolation>> interInvariantViolations = new HashMap<>();

    private final Set<TestStatus> detectedStatuses = new HashSet<>();

    /** record of every read/write effect of the run, in order of occurrence. */
    private final List<StepEffect> effectSequence = new ArrayList<>();

    /** functionalities for which a compensation path was injected */
    private final Set<FunctionalityId> compensatedFunctionalities = new HashSet<>();

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

        List<Anomaly> anomalies = analyzeAnomalies();

        return new TestResult(
                intraDependencies,
                interDependencies,
                functionalities,
                List.copyOf(schedule), // list will reflect the LinkedHashSet order
                stepExceptionsMap,
                detectedStatuses,
                effectSequence,
                ReadsFromRelation.deriveAll(effectSequence),
                anomalies,
                interInvariantViolations);
    }

    /**
     * Runs the {@link AnomalyAnalyzer} over the run's effect sequence.
     * <p>
     * Never throws: the analyzer is diagnostic metadata, and a bug in it must
     * not destroy the schedule/reads-from/invariant data the run already
     * earned. A crash is made unmissable instead — logged and flagged with
     * {@link TestStatus#ANOMALY_ANALYSIS_FAILED}, so the empty anomaly list can
     * never pass for "no anomalies found".
     */
    private List<Anomaly> analyzeAnomalies() {
        try {
            List<Anomaly> anomalies = AnomalyAnalyzer.analyze(
                    effectSequence, compensatedFunctionalities, committedFunctionalities());

            if (!anomalies.isEmpty()) {
                log.warn("Isolation anomalies detected: {}",
                        anomalies.stream().map(Anomaly::description).toList());
                detectedStatuses.add(TestStatus.ISOLATION_ANOMALY);
            }
            return anomalies;
        } catch (Exception e) {
            log.error("AnomalyAnalyzer crashed; "
                    + "this run's anomaly list will be empty and should be ignored", e);
            detectedStatuses.add(TestStatus.ANOMALY_ANALYSIS_FAILED);
            return List.of();
        }
    }

    /**
     * Functionalities whose work was durably committed: saga functionalities
     * whose {@link CommitStep} succeeded, and event-handler functionalities
     * whose single {@link EventHandlerStep} succeeded (event handlers commit
     * within their own step; the oracle adds no separate commit step for them).
     */
    private Set<FunctionalityId> committedFunctionalities() {
        Set<FunctionalityId> committed = new HashSet<>();
        for (OracleStep step : steps.values()) {
            boolean commitsItsFunctionality = switch (step) {
                case CommitStep commitStep -> true;
                case EventHandlerStep eventHandlerStep -> true;

                // steps that do not commit their functionality
                case FunctionalityStep functionalityStep -> false;
                case CompensationStep compensationStep -> false;
                case AbortStep abortStep -> false;
            };

            if (commitsItsFunctionality && successfulSteps.contains(step.getId())) {
                committed.add(step.getFunctionalityId());
            }
        }
        return committed;
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

            boolean stepWroteState = captureStepEffects(step);
            deferredEventRetryTracker.recordExecutedStep(step, stepWroteState);
            captureEmittedEventSteps(stepId);
        }
    }

    /**
     * Appends the effects the step just produced to the effect sequence.
     *
     * @return whether the step wrote aggregate state
     */
    private boolean captureStepEffects(OracleStep step) {
        boolean wroteState = false;
        for (Effect effect : traceSession.drain()) {
            effectSequence.add(
                    StepEffect.of(effectSequence.size(), step.getId(), StepKind.of(step), effect));
            if (effect instanceof Effect.Write) {
                wroteState = true;
            }
        }
        return wroteState;
    }

    private void captureEmittedEventSteps(StepId stepId) {
        // Run all event handling routines at once to capture events emitted by step.
        EventUtils.runEventHandlingScheduledTasks(eventHandlings);
        Set<DeferredEventInvocation> eventInvocations = captureSession.drain();

        // TODO should capture all events, or filter to selected EventHandlers for test?
        List<EventHandlerStep> eventHandlerSteps = new ArrayList<>();
        for (DeferredEventInvocation invocation : eventInvocations) {
            if (!deferredEventRetryTracker.canSchedule(invocation)) {
                continue;
            }

            EventHandlerStep eventHandlerStep = new EventHandlerStep(
                    invocation.event(),
                    invocation.handler(),
                    stepId,
                    invocation.publisherAggregateId(),
                    invocation.subscriberAggregateId());
            deferredEventRetryTracker.recordScheduled(invocation, eventHandlerStep.getId());
            eventHandlerSteps.add(eventHandlerStep);
        }

        addSteps(eventHandlerSteps);
    }

    /**
     * Prevents a deferred event from scheduling itself repeatedly when its handler
     * makes no progress, while allowing a later polling opportunity to retry it.
     */
    private static final class DeferredEventRetryTracker {
        /** Sentinel used while a scheduled event-handler step has not executed yet. */
        private static final long PENDING_ATTEMPT = -1L;

        private final Map<DeferredEventInvocation, EventAttempt> latestAttempts = new HashMap<>();
        private final Map<StepId, DeferredEventInvocation> invocationsByStep = new HashMap<>();
        /**
         * Counts progress boundaries used to decide whether a deferred invocation
         * may be retried. It advances after non-event steps and state-writing handlers.
         */
        private long pollingEpoch;

        /**
         * Returns whether a captured invocation should become a new event-handler step.
         * <p>
         * Pending attempts and attempts completed in the current polling epoch are
         * suppressed. A later epoch permits retrying the invocation.
         */
        boolean canSchedule(DeferredEventInvocation invocation) {
            EventAttempt previousAttempt = latestAttempts.get(invocation);
            return previousAttempt == null
                    || (previousAttempt.executedEpoch() != PENDING_ATTEMPT
                            && previousAttempt.executedEpoch() < pollingEpoch);
        }

        /**
         * Records a newly scheduled attempt so duplicate polls are suppressed until
         * it executes and becomes eligible for a later retry.
         */
        void recordScheduled(DeferredEventInvocation invocation, StepId eventStepId) {
            latestAttempts.put(invocation, new EventAttempt(eventStepId, PENDING_ATTEMPT));
            invocationsByStep.put(eventStepId, invocation);
        }

        /**
         * Records an executed step and advances the retry epoch when another poll may
         * see progress. Non-event steps always advance it; event-handler steps advance
         * it only when they write aggregate state.
         */
        void recordExecutedStep(OracleStep step, boolean wroteState) {
            if (step instanceof EventHandlerStep) {
                recordExecutedEventAttempt(step.getId());
                if (wroteState) {
                    pollingEpoch++;
                }
            } else {
                pollingEpoch++;
            }
        }

        /** Marks an event-handler attempt as executed in the current polling epoch. */
        private void recordExecutedEventAttempt(StepId eventStepId) {
            DeferredEventInvocation invocation = invocationsByStep.get(eventStepId);
            if (invocation == null) {
                throw new IllegalStateException("Missing deferred invocation for event-handler step '%s'"
                        .formatted(eventStepId));
            }

            EventAttempt latestAttempt = latestAttempts.get(invocation);
            if (latestAttempt == null || !latestAttempt.eventStepId().equals(eventStepId)) {
                throw new IllegalStateException("Event-handler step '%s' is not the latest scheduled attempt"
                        .formatted(eventStepId));
            }
            latestAttempts.put(invocation, new EventAttempt(eventStepId, pollingEpoch));
        }

        private record EventAttempt(StepId eventStepId, long executedEpoch) {
        }
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
        compensatedFunctionalities.add(funcId);
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

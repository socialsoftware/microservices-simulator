package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionException;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.utils.FunctionalityUtils;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.FlowStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;

class ScheduleExecutor {
    // TODO internal StepDependencyGraph could be more efficient and cleaner

    private static final int STEP_EXECUTION_LIMIT = 500;

    private final SagaUnitOfWorkService uowService;
    private final List<WorkflowFunctionality> functionalities = new ArrayList<>();
    private final StepDependencies interDependencies;
    private final StepDependencies intraDependencies = new StepDependencies();
    private final Set<FlowStep> allStepsFound = new HashSet<>();

    private final Map<FlowStep, WorkflowFunctionality> stepFunctionalityMap = new HashMap<>();
    private final List<FlowStep> schedule = new ArrayList<>();
    private final Set<FlowStep> successfulSteps = new HashSet<>();
    private final Map<FlowStep, Exception> stepExceptionsMap = new HashMap<>();
    private final Set<TestStatus> detectedStatuses = new HashSet<>();

    public ScheduleExecutor(
            List<WorkflowFunctionality> functionalities,
            StepDependencies interDependencies,
            SagaUnitOfWorkService uowService) {

        this.uowService = uowService;
        this.interDependencies = Objects.requireNonNull(new StepDependencies(interDependencies));
        for (WorkflowFunctionality func : functionalities) {
            addFunctionality(func);
        }

        // TODO review the allStepsFound concept, should it be changed?
        // allFoundSteps is the union of the steps that had intraDependencies with the
        // steps that had interDependencies which should represent all the steps that
        // were initially expected plus the ones found dynamically
        allStepsFound.addAll(interDependencies.getSteps());
    }

    private void addFunctionality(WorkflowFunctionality func) {
        functionalities.add(func);
        var workflowSteps = FunctionalityUtils.getSteps(func);

        intraDependencies.merge(StepDependencies.of(workflowSteps));

        for (FlowStep step : workflowSteps) {
            stepFunctionalityMap.put(step, func);
        }

        allStepsFound.addAll(workflowSteps);
    }

    public TestResult execute() {
        while (schedule.size() < STEP_EXECUTION_LIMIT) {
            var stepOpt = getNextStep();
            if (stepOpt.isEmpty()) {
                break;
            }

            FlowStep step = stepOpt.get();
            if (schedule.contains(step)) {
                // TODO change to step id when it is avaialable
                throw new IllegalStateException("Step %s cannot be executed more than once.".formatted(step.getName()));
            }

            WorkflowFunctionality func = stepFunctionalityMap.get(step);
            schedule.add(step);
            try {
                func.executeUntilStep(step.getName(), func.getWorkflow().getUnitOfWork());
                successfulSteps.add(step);
            } catch (Exception e) {
                if (e instanceof CompletionException ce && ce.getCause() instanceof Exception cause) {
                    // unwrap CompletionExceptions if they wrap Exception(excludes Throwables, null)
                    e = cause;
                }

                stepExceptionsMap.put(step, e);
                e.printStackTrace();

                // ! TODO verify that SimulatorException represents all the benign exceptions
                if (!(e instanceof SimulatorException)) {
                    detectedStatuses.add(TestStatus.INTERNAL_EXCEPTION);
                    break; // defensive break to not continue to test on a broken app state
                }

                var compensationFunc = new CompensationFunctionality(func, uowService);
                addFunctionality(compensationFunc);
            }
        }

        int totalSteps = allStepsFound.size();
        if (schedule.size() >= STEP_EXECUTION_LIMIT) {
            detectedStatuses.add(TestStatus.EXECUTION_LIMIT_EXCEEDED);
        } else if (schedule.size() < totalSteps) {
            // ! TODO review SCHEDULE_REJECTED and DEADLOCK statuses
            detectedStatuses.add(TestStatus.SCHEDULE_REJECTED);
        }

        return new TestResult(functionalities, schedule, stepExceptionsMap, detectedStatuses);
    }

    private Optional<FlowStep> getNextStep() {
        // TODO should be changed to pseudo-random pick (or determinisitic for testing)
        return getAvailableSteps().stream().findFirst();
    }

    private List<FlowStep> getAvailableSteps() {
        // TODO this could be better optimized
        return allStepsFound.stream()
                .filter(this::stepCanExecute)
                .toList();
    }

    private boolean stepCanExecute(FlowStep step) {
        return !schedule.contains(step) && stepDependenciesSatisfied(step);
    }

    private boolean stepDependenciesSatisfied(FlowStep step) {
        // intra-dependencies need to be successful to release
        boolean intraDepsSatisfied = successfulSteps.containsAll(intraDependencies.getStepDependencies(step));

        // inter-dependencies only need to have executed (successful or not) to release
        boolean interDepsSatisfied = schedule.containsAll(interDependencies.getStepDependencies(step));

        return intraDepsSatisfied && interDepsSatisfied;
    }
}

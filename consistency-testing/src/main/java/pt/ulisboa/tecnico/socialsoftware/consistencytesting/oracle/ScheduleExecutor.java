package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.utils.WorkflowUtils;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.FlowStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.Workflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

class ScheduleExecutor {
    private static final int STEP_EXECUTION_LIMIT = 500;

    private final List<WorkflowFunctionality> functionalities;
    private final StepDependencies interDependencies;
    private final StepDependencies intraDependencies = new StepDependencies();
    private final Set<FlowStep> allStepsFound;

    private final Map<FlowStep, WorkflowFunctionality> stepFunctionalityMap = new HashMap<>();
    private final List<FlowStep> schedule = new ArrayList<>();
    private final Set<FlowStep> successfulSteps = new HashSet<>();
    private final Map<FlowStep, Exception> stepExceptionsMap = new HashMap<>();
    private final Set<TestStatus> detectedStatuses = new HashSet<>();

    public ScheduleExecutor(
            List<WorkflowFunctionality> functionalities,
            StepDependencies interDependencies) {

        this.functionalities = Objects.requireNonNull(List.copyOf(functionalities));
        this.interDependencies = Objects.requireNonNull(interDependencies);
        initStepFuncMapAndIntraDependencies(functionalities);
        // allFoundSteps is the union of the steps that had intraDependencies with the
        // steps that had interDependencies which should represent all the steps that
        // were initially expected plus the ones found dynamically
        allStepsFound = new HashSet<>(intraDependencies.getSteps());
        allStepsFound.addAll(interDependencies.getSteps());
    }

    private void initStepFuncMapAndIntraDependencies(List<WorkflowFunctionality> functionalities) {
        for (WorkflowFunctionality func : functionalities) {
            Workflow workflow = func.getWorkflow();
            Objects.requireNonNull(workflow);

            for (FlowStep step : WorkflowUtils.getWorkflowSteps(workflow)) {
                intraDependencies.setStepDependencies(step, new HashSet<>(step.getDependencies()));
                stepFunctionalityMap.put(step, func);
            }
        }
    }

    public TestResult execute() {
        while (schedule.size() < STEP_EXECUTION_LIMIT) {
            var stepOpt = getNextStep();
            if (stepOpt.isEmpty()) {
                break;
            }

            FlowStep step = stepOpt.get();
            if (schedule.contains(step)) {
                throw new IllegalStateException("Step '" + step.getName() + "' cannot be executed more than once.");
            }

            schedule.add(step);
            try {
                WorkflowFunctionality func = stepFunctionalityMap.get(step);
                func.executeUntilStep(step.getName(), func.getWorkflow().getUnitOfWork());
                successfulSteps.add(step);
            } catch (Exception e) {
                stepExceptionsMap.put(step, e);
                e.printStackTrace();
                if (!(e instanceof SimulatorException)) {
                    detectedStatuses.add(TestStatus.INTERNAL_EXCEPTION);
                    break; // defensive break to not continue to test on a broken app state
                }
            }
        }

        int totalSteps = allStepsFound.size();
        if (schedule.size() >= STEP_EXECUTION_LIMIT) {
            detectedStatuses.add(TestStatus.EXECUTION_LIMIT_EXCEEDED);
        } else if (schedule.size() < totalSteps) {
            detectedStatuses.add(TestStatus.SCHEDULE_REJECTED);
        }

        return new TestResult(functionalities, schedule, stepExceptionsMap, detectedStatuses);
    }

    private Optional<FlowStep> getNextStep() {
        // TODO use random sampling, maybe keep a deterministic version for testing
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

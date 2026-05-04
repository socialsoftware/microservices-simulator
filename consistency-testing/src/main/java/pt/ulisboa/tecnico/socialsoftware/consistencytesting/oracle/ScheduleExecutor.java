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
    private final StepDependencies stepDependencies = new StepDependencies();
    private final Map<FlowStep, WorkflowFunctionality> stepFunctionalityMap = new HashMap<>();
    private final List<FlowStep> schedule = new ArrayList<>();
    private final Set<FlowStep> successfulSteps = new HashSet<>();
    private final Map<FlowStep, Exception> stepExceptionsMap = new HashMap<>();
    private final Set<TestStatus> detectedStatuses = new HashSet<>();

    public ScheduleExecutor(
            List<WorkflowFunctionality> functionalities,
            StepDependencies interDependencies) {

        this.functionalities = Objects.requireNonNull(List.copyOf(functionalities));
        initStepMaps(this.functionalities, interDependencies);
    }

    private void initStepMaps(
            List<WorkflowFunctionality> functionalities,
            StepDependencies interDependencies) {

        for (WorkflowFunctionality func : functionalities) {
            Workflow workflow = func.getWorkflow();
            Objects.requireNonNull(workflow);

            for (FlowStep step : WorkflowUtils.getWorkflowSteps(workflow)) {
                stepDependencies.setStepDependencies(step, new HashSet<>(step.getDependencies()));
                stepFunctionalityMap.put(step, func);
            }
        }

        // merge interleavings inter-functionality-dependencies into the existing
        // intra-functionality-dependencies (functionality's step sequence)
        stepDependencies.merge(interDependencies);
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

        int totalSteps = stepDependencies.getSteps().size();
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
        return stepDependencies.getSteps().stream()
                .filter(this::stepCanExecute)
                .toList();
    }

    private boolean stepCanExecute(FlowStep step) {
        return !schedule.contains(step) && stepDependenciesSatisfied(step);
    }

    private boolean stepDependenciesSatisfied(FlowStep step) {
        return successfulSteps.containsAll(step.getDependencies());
    }
}

package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
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

public class ScheduleExecutor {
    private static final int MAX_STEPS_EXECUTED = 500;

    private final List<WorkflowFunctionality> functionalities;
    private final Map<FlowStep, Set<FlowStep>> stepDependenciesMap = new HashMap<>();
    private final Map<FlowStep, WorkflowFunctionality> stepFunctionalityMap = new HashMap<>();
    private final Set<FlowStep> executedSteps = new LinkedHashSet<>(); // LinkedHashSet keeps insertion order
    private final Map<FlowStep, Exception> stepExceptionsMap = new HashMap<>();
    private final Set<TestStatus> detectedStatuses = new HashSet<>();

    public ScheduleExecutor(
            List<WorkflowFunctionality> functionalities,
            Map<FlowStep, Set<FlowStep>> interDependencies) {

        this.functionalities = Objects.requireNonNull(List.copyOf(functionalities));
        initStepMaps(this.functionalities, interDependencies);
    }

    private void initStepMaps(
            List<WorkflowFunctionality> functionalities,
            Map<FlowStep, Set<FlowStep>> interDependencies) {

        for (WorkflowFunctionality func : functionalities) {
            Workflow workflow = func.getWorkflow();
            Objects.requireNonNull(workflow);

            for (FlowStep step : WorkflowUtils.getWorkflowSteps(workflow)) {
                stepDependenciesMap.put(step, new HashSet<>(step.getDependencies()));
                stepFunctionalityMap.put(step, func);
            }
        }

        // merge interleavings inter-functionality-dependencies into the existing
        // intra-functionality-dependencies (functionality's step sequence)
        interDependencies.forEach((key, value) -> {
            stepDependenciesMap.merge(key, new HashSet<>(value), (dependenciesSet, interDependenciesSet) -> {
                dependenciesSet.addAll(interDependenciesSet);
                return dependenciesSet;
            });
        });
    }

    public TestResult execute() {
        int stepsExecuted = 0;

        while (stepsExecuted < MAX_STEPS_EXECUTED) {
            var stepOpt = getNextStep();
            if (stepOpt.isEmpty()) {
                break;
            }

            FlowStep step = stepOpt.get();
            try {
                WorkflowFunctionality func = stepFunctionalityMap.get(step);
                func.executeUntilStep(step.getName(), func.getWorkflow().getUnitOfWork());
            } catch (Exception e) {
                stepExceptionsMap.put(step, e);
                e.printStackTrace();
                if (!(e instanceof SimulatorException)) {
                    detectedStatuses.add(TestStatus.INTERNAL_EXCEPTION);
                    e.printStackTrace();
                    break; // defensive break to not continue to test on a broken app state
                }
            }

            executedSteps.add(step);
            stepsExecuted++;
        }

        int totalSteps = stepDependenciesMap.size();
        if (stepsExecuted >= MAX_STEPS_EXECUTED) {
            detectedStatuses.add(TestStatus.RUN_LIMIT_EXCEEDED);
        } else if (executedSteps.size() < totalSteps) {
            detectedStatuses.add(TestStatus.SCHEDULE_REJECTED);
        }

        List<FlowStep> schedule = new ArrayList<>(executedSteps);
        return new TestResult(schedule, stepExceptionsMap, detectedStatuses);
    }

    private Optional<FlowStep> getNextStep() {
        // TODO use random sampling, maybe keep a deterministic version for testing
        return getAvailableSteps().stream().findFirst();
    }

    private List<FlowStep> getAvailableSteps() {
        // TODO this could be better optimized
        return stepDependenciesMap.keySet().stream()
                .filter(this::stepCanExecute)
                .toList();
    }

    private boolean stepCanExecute(FlowStep step) {
        return !executedSteps.contains(step) && stepDependenciesSatisfied(step);
    }

    private boolean stepDependenciesSatisfied(FlowStep step) {
        return executedSteps.containsAll(step.getDependencies());
    }
}

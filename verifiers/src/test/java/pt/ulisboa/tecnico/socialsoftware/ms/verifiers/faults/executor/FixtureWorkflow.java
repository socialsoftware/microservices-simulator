package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import pt.ulisboa.tecnico.socialsoftware.ms.transactional.unitOfWork.UnitOfWork;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FixtureWorkflow {
    public static final List<String> STEPS = new ArrayList<>();
    public static int resumeCalls = 0;

    private final Object argument;

    public FixtureWorkflow(Object argument) {
        this.argument = argument;
    }

    public FixtureWorkflow(Object first, Object second, Object third) {
        this.argument = Arrays.asList(first, second, third);
    }

    public Object argument() {
        return argument;
    }

    public void executeUntilStep(String stepName, UnitOfWork unitOfWork) {
        STEPS.add(stepName);
        if ("fail".equals(stepName)) {
            throw new IllegalStateException("fixture failure");
        }
    }

    public void resumeWorkflow(UnitOfWork unitOfWork) {
        resumeCalls++;
    }
}

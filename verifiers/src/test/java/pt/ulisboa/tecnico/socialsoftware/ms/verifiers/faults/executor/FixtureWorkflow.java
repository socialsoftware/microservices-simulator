package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import pt.ulisboa.tecnico.socialsoftware.ms.faults.FaultVectorFault;
import pt.ulisboa.tecnico.socialsoftware.ms.faults.FaultVectorInjectedFaultException;
import pt.ulisboa.tecnico.socialsoftware.ms.faults.FaultVectorProviderHolder;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FixtureWorkflow {
    public static final List<String> STEPS = new ArrayList<>();
    public static final List<String> PARTICIPANT_STEPS = new ArrayList<>();
    public static final List<String> CLOSURES = new ArrayList<>();
    public static int resumeCalls = 0;
    public static int compensationCalls = 0;
    public static int constructorCalls = 0;
    public static final List<Object> constructorUnitOfWorks = new ArrayList<>();
    public static final List<Object> lifecycleUnitOfWorks = new ArrayList<>();
    public static boolean suppressFaultSignal = false;
    public static boolean injectUnexpectedSignal = false;
    public static boolean injectWrongSlotSignal = false;
    public static boolean compensationFails = false;
    public static final List<String> SUPPRESSED_STEPS = new ArrayList<>();

    private final Object argument;

    public FixtureWorkflow(Object argument) {
        constructorCalls++;
        this.argument = argument;
    }

    public FixtureWorkflow(Object first, Object second, Object third) {
        constructorCalls++;
        this.argument = Arrays.asList(first, second, third);
        constructorUnitOfWorks.add(third);
    }

    public Object argument() {
        return argument;
    }

    public void executeUntilStep(String stepName, UnitOfWork unitOfWork) {
        lifecycleUnitOfWorks.add(unitOfWork);
        String participant = String.valueOf(argument);
        FaultVectorProviderHolder.currentBoundary()
                .filter(context -> stepName.equals(context.runtimeStepName()))
                .ifPresent(context -> {
                    if (injectUnexpectedSignal) {
                        throw new FaultVectorInjectedFaultException(FaultVectorFault.from(context));
                    }
                    if (injectWrongSlotSignal) {
                        throw new FaultVectorInjectedFaultException(new FaultVectorFault(
                                context.scenarioExecutionId(),
                                context.scenarioPlanId(),
                                context.sagaInstanceId(),
                                context.scheduledStepId() + "-wrong",
                                context.slotIndex(),
                                context.functionalityClassFqn(),
                                context.functionalityClassSimpleName(),
                                context.runtimeStepName(),
                                context.assignedBit()));
                    }
                    if (!suppressFaultSignal && !SUPPRESSED_STEPS.contains(stepName)) {
                        FaultVectorProviderHolder.faultForCurrentBoundary().ifPresent(fault -> {
                            throw new FaultVectorInjectedFaultException(fault);
                        });
                    }
                });
        STEPS.add(stepName);
        PARTICIPANT_STEPS.add(participant + ":" + stepName);
        if ("fail".equals(stepName)) {
            throw new IllegalStateException("fixture failure");
        }
    }

    public void resumeWorkflow(UnitOfWork unitOfWork) {
        lifecycleUnitOfWorks.add(unitOfWork);
        CLOSURES.add(String.valueOf(argument));
        resumeCalls++;
    }

    public void resumeCompensation(UnitOfWork unitOfWork) {
        lifecycleUnitOfWorks.add(unitOfWork);
        compensationCalls++;
        if (compensationFails) {
            throw new IllegalStateException("fixture compensation failure");
        }
    }
}

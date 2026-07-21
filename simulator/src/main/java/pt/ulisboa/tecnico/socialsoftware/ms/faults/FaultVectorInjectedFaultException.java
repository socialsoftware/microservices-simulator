package pt.ulisboa.tecnico.socialsoftware.ms.faults;

import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

public class FaultVectorInjectedFaultException extends SimulatorException {
    private final String scenarioExecutionId;
    private final String scenarioPlanId;
    private final String sagaInstanceId;
    private final String scheduledStepId;
    private final int slotIndex;
    private final String functionalityClassFqn;
    private final String functionalityClassSimpleName;
    private final String runtimeStepName;
    private final int assignedBit;

    private FaultVectorInjectedFaultException(String errorTemplate, String formattedMessage, boolean alreadyFormatted) {
        super(errorTemplate, formattedMessage, alreadyFormatted);
        this.scenarioExecutionId = null;
        this.scenarioPlanId = null;
        this.sagaInstanceId = null;
        this.scheduledStepId = null;
        this.slotIndex = -1;
        this.functionalityClassFqn = null;
        this.functionalityClassSimpleName = null;
        this.runtimeStepName = null;
        this.assignedBit = -1;
    }

    public FaultVectorInjectedFaultException(FaultVectorFault fault) {
        super("Injected fault for scenario execution " + fault.scenarioExecutionId()
                + ", plan " + fault.scenarioPlanId()
                + ", slot " + fault.slotIndex()
                + ", scheduled step " + fault.scheduledStepId()
                + ", runtime step " + fault.runtimeStepName());
        this.scenarioExecutionId = fault.scenarioExecutionId();
        this.scenarioPlanId = fault.scenarioPlanId();
        this.sagaInstanceId = fault.sagaInstanceId();
        this.scheduledStepId = fault.scheduledStepId();
        this.slotIndex = fault.slotIndex();
        this.functionalityClassFqn = fault.functionalityClassFqn();
        this.functionalityClassSimpleName = fault.functionalityClassSimpleName();
        this.runtimeStepName = fault.runtimeStepName();
        this.assignedBit = fault.assignedBit();
    }

    public static FaultVectorInjectedFaultException fromRemote(String errorTemplate, String formattedMessage) {
        return new FaultVectorInjectedFaultException(errorTemplate, formattedMessage, true);
    }

    public String getScenarioExecutionId() {
        return scenarioExecutionId;
    }

    public String getScenarioPlanId() {
        return scenarioPlanId;
    }

    public String getSagaInstanceId() {
        return sagaInstanceId;
    }

    public String getScheduledStepId() {
        return scheduledStepId;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public String getFunctionalityClassFqn() {
        return functionalityClassFqn;
    }

    public String getFunctionalityClassSimpleName() {
        return functionalityClassSimpleName;
    }

    public String getRuntimeStepName() {
        return runtimeStepName;
    }

    public int getAssignedBit() {
        return assignedBit;
    }
}

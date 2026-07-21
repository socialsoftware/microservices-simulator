package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.FlowStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorDomainException;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;
import pt.ulisboa.tecnico.socialsoftware.ms.faults.FaultVectorFault;
import pt.ulisboa.tecnico.socialsoftware.ms.faults.FaultVectorInjectedFaultException;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FixtureWorkflow extends WorkflowFunctionality {
    public static final List<String> BODIES = new ArrayList<>();
    public static final List<String> COMPENSATIONS = new ArrayList<>();
    public static final Map<String, Integer> COMPENSATION_ATTEMPTS = new LinkedHashMap<>();
    public static final Map<String, SagaUnitOfWork> UNIT_OF_WORKS = new ConcurrentHashMap<>();
    private static final Set<String> BODY_DOMAIN_FAILURES = new HashSet<>();
    private static final Set<String> BODY_PLAIN_SIMULATOR_FAILURES = new HashSet<>();
    private static final Set<String> BODY_SERVICE_UNAVAILABLE_FAILURES = new HashSet<>();
    private static final Set<String> BODY_ASSIGNED_FAULT_LEAKS = new HashSet<>();
    private static final Set<String> BODY_INFRASTRUCTURE_FAILURES = new HashSet<>();
    private static final Set<String> EXPLICIT_REGISTRATION_BEFORE_FAILURES = new HashSet<>();
    private static final Set<String> IMPLICIT_STATE_STEPS = new HashSet<>();
    private static final Set<String> EXPLICIT_COMPENSATION_FAILURES = new HashSet<>();
    public static int constructorCalls;

    private final String participant;

    public FixtureWorkflow(Object participant,
                           SagaUnitOfWorkService unitOfWorkService,
                           SagaUnitOfWork unitOfWork) {
        constructorCalls++;
        this.participant = String.valueOf(participant);
        UNIT_OF_WORKS.put(this.participant, unitOfWork);

        SagaWorkflow sagaWorkflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);
        SagaStep first = step("first", unitOfWork, new ArrayList<>());
        SagaStep second = step("second", unitOfWork, new ArrayList<>(List.of(first)));
        SagaStep third = step("third", unitOfWork, new ArrayList<>(List.of(first)));
        sagaWorkflow.addStep(first);
        sagaWorkflow.addStep(second);
        sagaWorkflow.addStep(third);
        this.workflow = sagaWorkflow;
    }

    private SagaStep step(String name, SagaUnitOfWork unitOfWork, ArrayList<FlowStep> dependencies) {
        String key = this.participant + ":" + name;
        SagaStep step = dependencies.isEmpty()
                ? new SagaStep(name, () -> runBody(key, name, unitOfWork))
                : new SagaStep(name, () -> runBody(key, name, unitOfWork), dependencies);
        step.registerCompensation(() -> runCompensation(key), unitOfWork);
        return step;
    }

    private void runBody(String key, String stepName, SagaUnitOfWork unitOfWork) {
        BODIES.add(key);
        if (IMPLICIT_STATE_STEPS.contains(key)) {
            unitOfWork.savePreviousState(Math.abs(key.hashCode()), GenericSagaState.NOT_IN_SAGA);
        }
        if (EXPLICIT_REGISTRATION_BEFORE_FAILURES.contains(key)) {
            unitOfWork.registerCompensation(stepName, () -> runCompensation(key));
        }
        if (BODY_DOMAIN_FAILURES.contains(key)) {
            throw new SimulatorDomainException("fixture domain failure " + key);
        }
        if (BODY_PLAIN_SIMULATOR_FAILURES.contains(key)) {
            throw new SimulatorException("fixture unmarked simulator failure " + key);
        }
        if (BODY_SERVICE_UNAVAILABLE_FAILURES.contains(key)) {
            throw new SimulatorException("Service 'fixture' unavailable after retries exhausted for FixtureCommand: connection refused");
        }
        if (BODY_ASSIGNED_FAULT_LEAKS.contains(key)) {
            throw new FaultVectorInjectedFaultException(new FaultVectorFault(
                    "fixture-execution", "fixture-scenario", participant, key, 0,
                    FixtureWorkflow.class.getName(), FixtureWorkflow.class.getSimpleName(), stepName, 1));
        }
        if (BODY_INFRASTRUCTURE_FAILURES.contains(key)) {
            throw new IllegalStateException("fixture infrastructure failure " + key);
        }
    }

    private void runCompensation(String key) {
        COMPENSATION_ATTEMPTS.merge(key, 1, Integer::sum);
        COMPENSATIONS.add(key);
        if (EXPLICIT_COMPENSATION_FAILURES.contains(key)) {
            throw new IllegalStateException("fixture explicit compensation failure " + key);
        }
    }

    public static void failBodyWithDomainException(String participant, String stepName) {
        BODY_DOMAIN_FAILURES.add(participant + ":" + stepName);
    }

    public static void failBodyWithPlainSimulatorException(String participant, String stepName) {
        BODY_PLAIN_SIMULATOR_FAILURES.add(participant + ":" + stepName);
    }

    public static void failBodyWithServiceUnavailableException(String participant, String stepName) {
        BODY_SERVICE_UNAVAILABLE_FAILURES.add(participant + ":" + stepName);
    }

    public static void leakAssignedFaultException(String participant, String stepName) {
        BODY_ASSIGNED_FAULT_LEAKS.add(participant + ":" + stepName);
    }

    public static void failBodyWithInfrastructureException(String participant, String stepName) {
        BODY_INFRASTRUCTURE_FAILURES.add(participant + ":" + stepName);
    }

    public static void registerExplicitBeforeBodyFailure(String participant, String stepName) {
        EXPLICIT_REGISTRATION_BEFORE_FAILURES.add(participant + ":" + stepName);
    }

    public static void recordImplicitState(String participant, String stepName) {
        IMPLICIT_STATE_STEPS.add(participant + ":" + stepName);
    }

    public static void failExplicitCompensation(String participant, String stepName) {
        EXPLICIT_COMPENSATION_FAILURES.add(participant + ":" + stepName);
    }

    public static void allowExplicitCompensation(String participant, String stepName) {
        EXPLICIT_COMPENSATION_FAILURES.remove(participant + ":" + stepName);
    }

    public static void reset() {
        BODIES.clear();
        COMPENSATIONS.clear();
        COMPENSATION_ATTEMPTS.clear();
        UNIT_OF_WORKS.clear();
        BODY_DOMAIN_FAILURES.clear();
        BODY_PLAIN_SIMULATOR_FAILURES.clear();
        BODY_SERVICE_UNAVAILABLE_FAILURES.clear();
        BODY_ASSIGNED_FAULT_LEAKS.clear();
        BODY_INFRASTRUCTURE_FAILURES.clear();
        EXPLICIT_REGISTRATION_BEFORE_FAILURES.clear();
        IMPLICIT_STATE_STEPS.clear();
        EXPLICIT_COMPENSATION_FAILURES.clear();
        constructorCalls = 0;
    }
}

package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FixtureWorkflow extends WorkflowFunctionality {
    public static final List<String> BODIES = new ArrayList<>();
    public static final List<String> COMPENSATIONS = new ArrayList<>();
    public static final Map<String, SagaUnitOfWork> UNIT_OF_WORKS = new ConcurrentHashMap<>();
    public static int constructorCalls;

    private final String participant;

    public FixtureWorkflow(Object participant,
                           SagaUnitOfWorkService unitOfWorkService,
                           SagaUnitOfWork unitOfWork) {
        constructorCalls++;
        this.participant = String.valueOf(participant);
        UNIT_OF_WORKS.put(this.participant, unitOfWork);

        SagaWorkflow sagaWorkflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);
        SagaStep first = step("first", unitOfWork);
        SagaStep second = new SagaStep("second", () -> BODIES.add(this.participant + ":second"),
                new ArrayList<>(List.of(first)));
        second.registerCompensation(() -> COMPENSATIONS.add(this.participant + ":second"), unitOfWork);
        SagaStep third = new SagaStep("third", () -> BODIES.add(this.participant + ":third"),
                new ArrayList<>(List.of(first)));
        third.registerCompensation(() -> COMPENSATIONS.add(this.participant + ":third"), unitOfWork);
        sagaWorkflow.addStep(first);
        sagaWorkflow.addStep(second);
        sagaWorkflow.addStep(third);
        this.workflow = sagaWorkflow;
    }

    private SagaStep step(String name, SagaUnitOfWork unitOfWork) {
        SagaStep step = new SagaStep(name, () -> BODIES.add(this.participant + ":" + name));
        step.registerCompensation(() -> COMPENSATIONS.add(this.participant + ":" + name), unitOfWork);
        return step;
    }

    public static void reset() {
        BODIES.clear();
        COMPENSATIONS.clear();
        UNIT_OF_WORKS.clear();
        constructorCalls = 0;
    }
}

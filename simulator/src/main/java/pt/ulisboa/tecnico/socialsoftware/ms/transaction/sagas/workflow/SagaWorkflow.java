package pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.ExecutionPlan;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.FlowStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.Workflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWorkService;

import java.util.*;

public class SagaWorkflow extends Workflow {
    public SagaWorkflow(WorkflowFunctionality functionality, UnitOfWorkService unitOfWorkService, SagaUnitOfWork unitOfWork) {
        super(functionality, unitOfWorkService, unitOfWork);
    }

    @Override
    public ExecutionPlan planOrder(HashMap<FlowStep, ArrayList<FlowStep>> stepsWithDependencies) {
        ArrayList<FlowStep> orderedSteps = new ArrayList<>();
        HashMap<FlowStep, Integer> inDegree = new HashMap<>();
        Queue<FlowStep> readySteps = new LinkedList<>();

        // calcular quantas dependencias tem cada step
        for (HashMap.Entry<FlowStep, ArrayList<FlowStep>> entry: stepsWithDependencies.entrySet()) {
            inDegree.put(entry.getKey(), entry.getValue().size());
        }

        // os steps sem dependencias estao prontos para ser ordenados
        for (HashMap.Entry<FlowStep, Integer> entry: inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                readySteps.add(entry.getKey());
            }
        }

        while (!readySteps.isEmpty()) {
            FlowStep step = readySteps.poll();
            orderedSteps.add(step);

            for (HashMap.Entry<FlowStep, ArrayList<FlowStep>> entry: stepsWithDependencies.entrySet()) {
                if (!entry.getKey().equals(step) && entry.getValue().contains(step)) {
                    inDegree.put(entry.getKey(), inDegree.get(entry.getKey()) - 1); // se o passo ordenado for uma dependencia de outro reduzir o numero de dependencias desse step
                    if (inDegree.get(entry.getKey()) == 0) { // se deixou de ter dependencias esta pronto a ser ordenado
                        readySteps.add(entry.getKey());
                    }
                }
            }
        }

        if (orderedSteps.size() != stepsWithDependencies.size()) {
            throw new IllegalStateException("Cyclic dependency detected in steps");
        }

        return new ExecutionPlan(orderedSteps, stepsWithDependencies, this.getFunctionality());
    }

    @Override
    public void compensateUntilStep(String stepName, UnitOfWork unitOfWork) {
        if (!this.aborted) {
            throw new IllegalStateException("Cannot execute compensations because the workflow has not aborted.");
        }
        SagaUnitOfWork sagaUnitOfWork = (SagaUnitOfWork) unitOfWork;
        SagaUnitOfWorkService sagaService = (SagaUnitOfWorkService) unitOfWorkService;
        sagaService.abortUntilStep(sagaUnitOfWork, stepName);
    }

    @Override
    public void resumeCompensation(UnitOfWork unitOfWork) {
        if (!this.aborted) {
            throw new IllegalStateException("Cannot execute compensations because the workflow has not aborted.");
        }
        SagaUnitOfWork sagaUnitOfWork = (SagaUnitOfWork) unitOfWork;
        SagaUnitOfWorkService sagaService = (SagaUnitOfWorkService) unitOfWorkService;
        sagaService.abort(sagaUnitOfWork);
    }
}

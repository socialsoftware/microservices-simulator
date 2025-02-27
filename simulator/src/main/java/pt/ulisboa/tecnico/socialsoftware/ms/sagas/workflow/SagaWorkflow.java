package pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.ExecutionPlan;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.FlowStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Workflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;

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
}

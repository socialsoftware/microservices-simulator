package pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.ExecutionPlan;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.FlowStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Workflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowData;

public class SagaWorkflow extends Workflow {
    public SagaWorkflow(WorkflowData data, UnitOfWorkService unitOfWorkService, String functionalityName) {
        super(data, unitOfWorkService, functionalityName);
    }

    @Override
    public ExecutionPlan planOrder(HashMap<FlowStep, ArrayList<FlowStep>> stepsWithDependencies) {
        //TODO
        // mapear cada step com o numero de dependencias que tem
        // quando um step nao tiver dependencias por na queue de ordenados/comecar
        // cada vez que um passo for concluido reduzir o numero de dependencias do passos que dependiam dele
        // repetir ate a lista de steps por ordenar ficar vazia

        ArrayList<FlowStep> orderedSteps = new ArrayList<>();
        HashMap<FlowStep, Integer> inDegree = new HashMap<>();
        Queue<FlowStep> readySteps = new LinkedList<>();

        for (FlowStep step : stepsWithDependencies.keySet()) {
            inDegree.put(step, 0);
        }

        for (ArrayList<FlowStep> dependencies : stepsWithDependencies.values()) {
            for (FlowStep dependency : dependencies) {
                inDegree.put(dependency, inDegree.getOrDefault(dependency, 0) + 1);
            }
        }
        
        for (HashMap.Entry<FlowStep, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                readySteps.add(entry.getKey());
            }
        }

        // Process steps with no dependencies first
        while (!readySteps.isEmpty()) {
            FlowStep step = readySteps.poll();
            orderedSteps.add(step);

            for (FlowStep dependency : stepsWithDependencies.getOrDefault(step, new ArrayList<>())) {
                inDegree.put(dependency, inDegree.get(dependency) - 1);
                if (inDegree.get(dependency) == 0) {
                    readySteps.add(dependency);
                }
            }
        }

        if (orderedSteps.size() != stepsWithDependencies.size()) {
            throw new IllegalStateException("Cyclic dependency detected in steps");
        }

        // TODO 
        // logica para async e 
        // atualizar dependencias no lado do execute live durante execucao 
        // em vez de ter uma queue pre determinada estaticamente

        return new ExecutionPlan(orderedSteps);
    } 
}

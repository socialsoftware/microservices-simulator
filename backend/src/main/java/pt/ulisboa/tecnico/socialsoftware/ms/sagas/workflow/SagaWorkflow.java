package pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow;

import java.util.ArrayList;
import java.util.HashMap;

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
        return null;
    }
}

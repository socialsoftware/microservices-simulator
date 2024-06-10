package pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;

import java.util.*;

import org.springframework.context.annotation.Profile;

@Profile("sagas")
public class SagaUnitOfWork extends UnitOfWork {
    private ArrayList<Runnable> compensatingActions;

    public SagaUnitOfWork(Integer version, String functionalityName) {
        super(version, functionalityName);
        this.compensatingActions = new ArrayList<>();
    }

    public void registerCompensation(Runnable compensationAction) {
        this.compensatingActions.add(compensationAction);
    }

    public void compensate() {
        Collections.reverse(this.compensatingActions);
        for (Runnable action: compensatingActions) {
            action.run();
        }
    }

}

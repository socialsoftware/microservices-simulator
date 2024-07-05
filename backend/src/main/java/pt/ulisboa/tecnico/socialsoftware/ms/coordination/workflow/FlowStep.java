package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;

public interface FlowStep {
    public CompletableFuture<Void> execute(UnitOfWork unitOfWork);
    public void registerCompensation(Runnable compensationLogic, UnitOfWork unitOfWork);
    public Runnable getCompensation();
    public void setDependencies(ArrayList<FlowStep> dependencies);
    public ArrayList<FlowStep> getDependencies();
}

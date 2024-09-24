package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;

public class AsyncStep implements FlowStep {
    private String stepName;
    private Supplier<CompletableFuture<Void>> asyncOperation;
    private Runnable compensationLogic;
    private ExecutorService executorService;
    private ArrayList<FlowStep> dependencies = new ArrayList<>();

    private static final ExecutorService DEFAULT_EXECUTOR = Executors.newCachedThreadPool();

    public AsyncStep(String stepName, Supplier<CompletableFuture<Void>> asyncOperation, ArrayList<FlowStep> dependencies, ExecutorService executorService) {
        this.stepName = stepName;
        this.asyncOperation = asyncOperation;
        this.dependencies = dependencies;
        this.executorService = executorService;
    }

    public AsyncStep(String stepName, Supplier<CompletableFuture<Void>> asyncOperation, ExecutorService executorService) {
        this.stepName = stepName;
        this.asyncOperation = asyncOperation;
        this.executorService = executorService;
    }

    public AsyncStep(String stepName, Supplier<CompletableFuture<Void>> asyncOperation) {
        this.stepName = stepName;
        this.asyncOperation = asyncOperation;
        this.executorService = DEFAULT_EXECUTOR;
    }

    public AsyncStep(String stepName, Supplier<CompletableFuture<Void>> asyncOperation, ArrayList<FlowStep> dependencies) {
        this.stepName = stepName;
        this.asyncOperation = asyncOperation;
        this.dependencies = dependencies;
        this.executorService = DEFAULT_EXECUTOR;
    }

    @Override
    public CompletableFuture<Void> execute(UnitOfWork unitOfWork) {
        try {
            if (compensationLogic != null) {
                ((SagaUnitOfWork)unitOfWork).registerCompensation(compensationLogic);
            }
            return CompletableFuture.supplyAsync(asyncOperation, executorService).thenCompose(Function.identity());
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public void registerCompensation(Runnable compensationLogic, UnitOfWork unitOfWork) {
        this.compensationLogic = compensationLogic;
    }

    @Override
    public ArrayList<FlowStep> getDependencies() {
        return this.dependencies;
    }

    @Override
    public Runnable getCompensation() {
        return this.compensationLogic;
    }

    @Override
    public void setDependencies(ArrayList<FlowStep> dependencies) {
        this.dependencies = dependencies;
    }

    @Override
    public String getName() {
        return this.stepName;
    }
}
package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;

public class AsyncStep extends FlowStep {
    private Supplier<CompletableFuture<Void>> asyncOperation;
    private ExecutorService executorService;

    private static final ExecutorService DEFAULT_EXECUTOR = Executors.newCachedThreadPool();

    public AsyncStep(String stepName, Supplier<CompletableFuture<Void>> asyncOperation, ArrayList<FlowStep> dependencies, ExecutorService executorService) {
        super(stepName, dependencies);
        this.asyncOperation = asyncOperation;
        this.executorService = executorService;
    }

    public AsyncStep(String stepName, Supplier<CompletableFuture<Void>> asyncOperation, ExecutorService executorService) {
        super(stepName);
        this.asyncOperation = asyncOperation;
        this.executorService = executorService;
    }

    public AsyncStep(String stepName, Supplier<CompletableFuture<Void>> asyncOperation) {
        super(stepName);
        this.asyncOperation = asyncOperation;
        this.executorService = DEFAULT_EXECUTOR;
    }

    public AsyncStep(String stepName, Supplier<CompletableFuture<Void>> asyncOperation, ArrayList<FlowStep> dependencies) {
        super(stepName, dependencies);
        this.asyncOperation = asyncOperation;
        this.executorService = DEFAULT_EXECUTOR;
    }

    @Override
    public CompletableFuture<Void> execute(UnitOfWork unitOfWork) {
        try {
            if (getCompensation() != null) {
                ((SagaUnitOfWork)unitOfWork).registerCompensation(getCompensation());
            }
            return CompletableFuture.supplyAsync(asyncOperation, executorService).thenCompose(Function.identity());
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}
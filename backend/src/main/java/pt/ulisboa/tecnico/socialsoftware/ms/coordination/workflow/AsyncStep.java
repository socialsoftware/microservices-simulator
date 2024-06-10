package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;

public class AsyncStep implements FlowStep {
    private Supplier<CompletableFuture<Void>> asyncOperation;
    private Runnable compensationLogic;
    private ExecutorService executorService;

    private static final ExecutorService DEFAULT_EXECUTOR = Executors.newCachedThreadPool();

    public AsyncStep(Supplier<CompletableFuture<Void>> asyncOperation, ExecutorService executorService) {
        this.asyncOperation = asyncOperation;
        this.executorService = executorService;
    }

    public AsyncStep(Supplier<CompletableFuture<Void>> asyncOperation) {
        this.asyncOperation = asyncOperation;
        this.executorService = DEFAULT_EXECUTOR;
    }

    @Override
    public CompletableFuture<Void> execute() {
        return CompletableFuture.supplyAsync(asyncOperation, executorService)
                .thenCompose(Function.identity());
    }

    @Override
    public void registerCompensation(Runnable compensationLogic, UnitOfWork unitOfWork) {
        this.compensationLogic = compensationLogic;
        unitOfWork.registerCompensation(compensationLogic);
    }
}
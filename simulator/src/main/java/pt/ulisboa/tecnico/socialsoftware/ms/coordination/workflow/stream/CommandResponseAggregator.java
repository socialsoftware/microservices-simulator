package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.stream;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("stream")
public class CommandResponseAggregator {
    private final Map<String, CompletableFuture<CommandResponse>> pendingResponses = new ConcurrentHashMap<>();

    public CompletableFuture<CommandResponse> createResponseFuture(String correlationId) {
        CompletableFuture<CommandResponse> future = new CompletableFuture<>();
        pendingResponses.put(correlationId, future);
        return future;
    }

    public void completeResponse(String correlationId, CommandResponse response) {
        CompletableFuture<CommandResponse> future = pendingResponses.remove(correlationId);
        if (future != null) {
            future.complete(response);
        }
    }

    public void completeExceptionally(String correlationId, Throwable exception) {
        CompletableFuture<CommandResponse> future = pendingResponses.remove(correlationId);
        if (future != null) {
            future.completeExceptionally(exception);
        }
    }
}

package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.stream;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("stream")
public class CommandResponseAggregator {
    private final Map<String, CompletableFuture<Object>> pendingResponses = new ConcurrentHashMap<>();

    public CompletableFuture<Object> createResponseFuture(String correlationId) {
        CompletableFuture<Object> future = new CompletableFuture<>();
        pendingResponses.put(correlationId, future);
        return future;
    }

    public void completeResponse(String correlationId, Object response) {
        CompletableFuture<Object> future = pendingResponses.remove(correlationId);
        if (future != null) {
            future.complete(response);
        }
    }

    public void completeExceptionally(String correlationId, Throwable exception) {
        CompletableFuture<Object> future = pendingResponses.remove(correlationId);
        if (future != null) {
            future.completeExceptionally(exception);
        }
    }
}

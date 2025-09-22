package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;

import java.util.concurrent.CompletableFuture;

public interface CommandGateway {
    Object send(Command command);
    CompletableFuture<Object> sendAsync(Command command);
}

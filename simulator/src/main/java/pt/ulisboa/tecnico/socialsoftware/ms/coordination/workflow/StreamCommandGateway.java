package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;


@Component
public class StreamCommandGateway {
    private static final Logger logger = Logger.getLogger(StreamCommandGateway.class.getName());
    private final StreamBridge streamBridge;
    private final CommandResponseAggregator responseAggregator;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Autowired
    public StreamCommandGateway(StreamBridge streamBridge, CommandResponseAggregator responseAggregator, ObjectMapper objectMapper) {
        this.streamBridge = streamBridge;
        this.responseAggregator = responseAggregator;
        this.objectMapper = objectMapper;
    }

    public Object send(Command command) {
        String destination = command.getServiceName().toLowerCase() + "-command-channel";
        String correlationId = java.util.UUID.randomUUID().toString();

        // Register this request for a response
        CompletableFuture<Object> responseFuture = responseAggregator.createResponseFuture(correlationId);
        System.out.println("Sending command to " + destination);
        String json = null;
        try {
            json = objectMapper.writeValueAsString(command);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        logger.info(json);
        boolean sent = streamBridge.send(destination,
                MessageBuilder.withPayload(json)
                        .setHeader("correlationId", correlationId)
                        .setHeader("contentType", "application/json")
                        .build());

        if (!sent) {
            throw new RuntimeException("Failed to send command to service: " + command.getServiceName());
        }

        System.out.println("Command sent to " + destination);

        try {
            // Wait for the response
            Object response = responseFuture.get();
            logger.info("GOT RESPONSE: " + response);
            if (response instanceof SimulatorException) {
                throw (SimulatorException) response;
            }
            return response;
        } catch (Exception e) {
            logger.warning("Error while waiting for response: " + e.getMessage());
            throw new RuntimeException("Error processing command", e);
        }
    }

    public CompletableFuture<Object> sendAsync(Command command) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return send(command);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }

}

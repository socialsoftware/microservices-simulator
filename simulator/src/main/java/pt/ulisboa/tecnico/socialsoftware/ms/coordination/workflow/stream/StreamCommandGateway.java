package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.LocalCommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

@Component
@Profile("stream")
public class StreamCommandGateway implements CommandGateway {
    private static final Logger logger = Logger.getLogger(StreamCommandGateway.class.getName());
    private final StreamBridge streamBridge;
    private final CommandResponseAggregator responseAggregator;
    private final ObjectMapper msgMapper;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final ApplicationContext applicationContext;

    @Autowired
    public StreamCommandGateway(StreamBridge streamBridge,
            CommandResponseAggregator responseAggregator,
            MessagingObjectMapperProvider mapperProvider, ApplicationContext applicationContext) {
        this.streamBridge = streamBridge;
        this.responseAggregator = responseAggregator;
        this.msgMapper = mapperProvider.newMapper();
        this.applicationContext = applicationContext;
    }

    public Object send(Command command) {
        String service = command.getServiceName() != null ? command.getServiceName().toLowerCase() : "";
        String cmdPkg = command.getClass().getPackage().getName();
        boolean isSameServicePackage = !service.isEmpty() && (cmdPkg.contains(".command." + service));

        if (isSameServicePackage) {
            logger.info("Routing to LocalCommandGateway for command: " + command.getClass().getSimpleName());
            String handlerClassName = command.getServiceName() + "CommandHandler";

            CommandHandler handler = applicationContext.getBeansOfType(CommandHandler.class)
                    .values()
                    .stream()
                    .filter(h -> h.getClass().getSimpleName().equalsIgnoreCase(handlerClassName))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No handler found for command: " + handlerClassName));

            try {
                Object returnObject = handler.handle(command);
                if (returnObject instanceof SimulatorException) {
                    throw (SimulatorException) returnObject;
                }
                return returnObject;
            } catch (SimulatorException e) {
                Logger.getLogger(LocalCommandGateway.class.getName()).warning(e.getMessage());
                throw e;
            }
        }

        String destination = service + "-command-channel";
        String correlationId = java.util.UUID.randomUUID().toString();

        CompletableFuture<Object> responseFuture = responseAggregator.createResponseFuture(correlationId);
        logger.info("Sending command to " + destination);
        String json;
        try {
            json = msgMapper.writeValueAsString(command);
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

        logger.info("Command sent to " + destination);

        try {
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

package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.stream;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
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
    // Use a dedicated mapper for messaging to avoid affecting MVC serialization
    private final ObjectMapper msgMapper;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Autowired
    public StreamCommandGateway(StreamBridge streamBridge, CommandResponseAggregator responseAggregator,
            ObjectMapper objectMapper) {
        this.streamBridge = streamBridge;
        this.responseAggregator = responseAggregator;

        // Create an isolated mapper for stream messages
        this.msgMapper = objectMapper.copy();
        this.msgMapper.findAndRegisterModules();

        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("pt.ulisboa.tecnico")
                .build();

        // Apply typing only to our domain POJOs; skip maps/collections/arrays/etc.
        TypeResolverBuilder<?> typer = new ObjectMapper.DefaultTypeResolverBuilder(
                ObjectMapper.DefaultTyping.NON_FINAL, ptv) {
            @Override
            public boolean useForType(JavaType t) {
                if (t.isPrimitive() || t.isEnumType())
                    return false;
                if (t.isArrayType() || t.isCollectionLikeType() || t.isMapLikeType() || t.isContainerType())
                    return false;
                Class<?> raw = t.getRawClass();
                Package p = raw.getPackage();
                String pkg = (p == null) ? "" : p.getName();
                return pkg.startsWith("pt.ulisboa.tecnico");
            }
        };
        // ensures "@class" is used
        typer = typer.init(JsonTypeInfo.Id.CLASS, null).inclusion(JsonTypeInfo.As.PROPERTY);

        this.msgMapper.setDefaultTyping(typer);
    }

    public Object send(Command command) {
        String destination = command.getServiceName().toLowerCase() + "-command-channel";
        String correlationId = java.util.UUID.randomUUID().toString();

        CompletableFuture<Object> responseFuture = responseAggregator.createResponseFuture(correlationId);
        System.out.println("Sending command to " + destination);
        String json = null;
        try {
            // Serialize with the messaging mapper (includes @class for nested DTOs)
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

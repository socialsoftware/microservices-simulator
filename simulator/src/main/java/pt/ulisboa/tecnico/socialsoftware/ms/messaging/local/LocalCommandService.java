package pt.ulisboa.tecnico.socialsoftware.ms.messaging.local;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandResponse;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.MessagingObjectMapperProvider;

import java.util.UUID;
import java.util.logging.Logger;

@Component
@Profile("local")
public class LocalCommandService {

    private static final Logger logger = Logger.getLogger(LocalCommandService.class.getName());

    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper;

    @Autowired
    public LocalCommandService(ApplicationContext applicationContext, MessagingObjectMapperProvider mapperProvider) {
        this.applicationContext = applicationContext;
        this.objectMapper = mapperProvider.newMapper();
    }

    public String sendJson(String commandJson) {
        Command command;
        try {
            command = objectMapper.readValue(commandJson, Command.class);
        } catch (Exception e) {
            logger.severe("Failed to deserialize command: " + e.getMessage());
            CommandResponse response = CommandResponse.error(null, e, null);
            try {
                return objectMapper.writeValueAsString(response);
            } catch (JsonProcessingException ex) {
                throw new RuntimeException("Failed to serialize error response", ex);
            }
        }

        String correlationId = UUID.randomUUID().toString();
        CommandResponse response;
        try {
            Object result = send(command);
            response = CommandResponse.success(correlationId, result, command.getUnitOfWork());
        } catch (SimulatorException e) {
            logger.warning("Command handling error: " + e.getMessage());
            response = CommandResponse.error(correlationId, e, command.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Unexpected error handling command: " + e.getMessage() + " " + command.getClass().getSimpleName());
            response = CommandResponse.error(correlationId, e, command.getUnitOfWork());
        }

        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize success response", e);
        }
    }

    public Object send(Command command) {
        CommandHandler handler;

        try {
            handler = (CommandHandler) applicationContext.getBean(command.getServiceName() + "CommandHandler");
        } catch (Exception e) {
            logger.severe("Failed to find command handler for service: " + command.getServiceName());
            throw new RuntimeException("Failed to find command handler", e);
        }

        logger.info("Delegating local command to handler for service: " + command.getServiceName());
        return handler.handle(command);
    }
}

package pt.ulisboa.tecnico.socialsoftware.ms.messaging.local;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandResponse;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic.DynamicEvidenceProperties;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic.DynamicEvidenceRecorderHolder;

@Component
@Profile("local")
public class LocalCommandGateway extends CommandGateway {

    private final LocalCommandService localCommandService;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private DynamicEvidenceProperties dynamicEvidenceProperties = new DynamicEvidenceProperties();

    @Value("${local.messaging.serialize:false}")
    private boolean serializeMessages;

    @Autowired
    public LocalCommandGateway(ApplicationContext applicationContext, RetryRegistry retryRegistry, LocalCommandService localCommandService, MessagingObjectMapperProvider mapperProvider) {
        super(applicationContext, retryRegistry);
        this.localCommandService = localCommandService;
        this.objectMapper = mapperProvider.newMapper();
    }

    public LocalCommandGateway(ApplicationContext applicationContext, RetryRegistry retryRegistry) {
        this(applicationContext, retryRegistry,
                new LocalCommandService(applicationContext, new MessagingObjectMapperProvider(new ObjectMapper())),
                new MessagingObjectMapperProvider(new ObjectMapper()));
    }

    @Override
    @Retry(name = "commandGateway", fallbackMethod = "fallbackSend")
    public Object send(Command command) {
        try {
            DynamicEvidenceRecorderHolder.recordCommandSent(command, dynamicEvidenceProperties);
        } catch (RuntimeException e) {
            logger.warning("Failed to record dynamic evidence COMMAND_SENT: " + e.getMessage());
        }

        logger.info("Executing command via LocalCommandService: " + command.getClass().getSimpleName() + " (serialization=" + serializeMessages + ")");

        if (serializeMessages) {
            CommandResponse response;
            String commandJson;
            try {
                commandJson = objectMapper.writeValueAsString(command);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize command", e);
            }

            String responseJson = localCommandService.sendJson(commandJson);

            try {
                response = objectMapper.readValue(responseJson, CommandResponse.class);
            } catch (Exception e) {
                throw new RuntimeException("Failed to deserialize command response", e);
            }

            mergeUnitOfWork(command.getUnitOfWork(), response.unitOfWork());

            if (response.isError()) {
                throwMatchingException(response.errorType(), response.errorMessage(), response.errorTemplate());
            }
            return response.result();
        } else {
            return localCommandService.send(command);
        }
    }
}

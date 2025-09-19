package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import java.util.function.Consumer;
import java.util.logging.Logger;

@Component
public class CommandResponseListener {

    private static final Logger logger = Logger.getLogger(CommandResponseListener.class.getName());
    private final CommandResponseAggregator responseAggregator;

    @Autowired
    public CommandResponseListener(CommandResponseAggregator responseAggregator) {
        this.responseAggregator = responseAggregator;
    }

    @Bean
    public Consumer<Message<CommandResponse>> commandResponseChannel() {
        logger.info("CommandResponseListener: Starting command response channel");
        return message -> {
            CommandResponse response = message.getPayload();
            logger.info("Received response for correlation ID: " + response.correlationId());

            if (response.isError()) {
                responseAggregator.completeExceptionally(
                        response.correlationId(),
                        new SimulatorException(response.errorMessage())
                );
            } else {
                responseAggregator.completeResponse(
                        response.correlationId(),
                        response.result()
                );
            }
        };
    }
}

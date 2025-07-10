package pt.ulisboa.tecnico.socialsoftware.quizzes;


import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;

@Configuration
public class CommandConfig {
    @Bean
    public CommandGateway commandGateway(ApplicationContext applicationContext) {
        return new CommandGateway(applicationContext);
    }
}

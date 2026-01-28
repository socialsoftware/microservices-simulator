package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscriberService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionRepository;

import java.util.function.Consumer;

@Component
@Profile("!local")
public class CourseExecutionEventSubscriberService extends EventSubscriberService {

    @Autowired
    private CourseExecutionRepository courseExecutionRepository;

    public CourseExecutionEventSubscriberService(EventRepository eventRepository,
            MessagingObjectMapperProvider mapperProvider) {
        super(eventRepository, mapperProvider);
    }

    @Override
    public Aggregate getLatestAggregate() {
        return courseExecutionRepository.findLatestCourseExecution().orElse(null);
    }

    @Override
    public String getEventPackage() {
        return "pt.ulisboa.tecnico.socialsoftware.quizzes.events";
    }

    @Bean
    public Consumer<Message<String>> courseExecutionEventSubscriber() {
        return this::processEvent;
    }
}

package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.messaging.Message;
import java.util.function.Consumer;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscriberService;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.stream.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.CourseRepository;

@Component
@Profile("stream")
public class CourseEventSubscriberService extends EventSubscriberService {

    @Autowired
    private CourseRepository courseRepository;

    public CourseEventSubscriberService(EventRepository eventRepository, MessagingObjectMapperProvider mapperProvider) {
        super(eventRepository, mapperProvider);
    }

    @Override
    public Aggregate getLatestAggregate() {
        return courseRepository.findLatestCourse().orElse(null);
    }

    @Override
    public String getEventPackage() {
        return "pt.ulisboa.tecnico.socialsoftware.quizzes.events";
    }

    @Bean
    public Consumer<Message<String>> courseEventSubscriber() {
        return this::processEvent;
    }
}

package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.stream.MessagingObjectMapperProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscriberService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentRepository;

import java.util.function.Consumer;

@Component
@Profile("!local")
public class TournamentEventSubscriberService extends EventSubscriberService {

    @Autowired
    private TournamentRepository tournamentRepository;

    public TournamentEventSubscriberService(EventRepository eventRepository,
            MessagingObjectMapperProvider mapperProvider) {
        super(eventRepository, mapperProvider);
    }

    @Override
    public Aggregate getLatestAggregate() {
        return tournamentRepository.findLatestTournament().orElse(null);
    }

    @Override
    public String getEventPackage() {
        return "pt.ulisboa.tecnico.socialsoftware.quizzes.events";
    }

    @Bean
    public Consumer<Message<String>> tournamentEventSubscriber() {
        return this::processEvent;
    }
}

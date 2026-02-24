package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.events.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.events.handling.TournamentEventHandling;

@RestController
public class TournamentEventController {
    @Autowired
    private TournamentEventHandling tournamentEventHandling;

    @PostMapping("/events/topic/update")
    public void processUpdateTopic() {
        tournamentEventHandling.handleUpdateTopicEvents();
    }

    @PostMapping(value = "/tournament/process/anonymize")
    public void processAnonymize() {
        tournamentEventHandling.handleAnonymizeStudentEvents();
    }

    @PostMapping(value = "/tournament/process/updateExecutionStudentName")
    public void processUpdateExecutionName() {
        tournamentEventHandling.handleUpdateStudentNameEvent();
    }
}

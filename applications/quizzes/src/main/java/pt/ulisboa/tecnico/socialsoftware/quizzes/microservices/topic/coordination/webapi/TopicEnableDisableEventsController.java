package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.webapi;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/scheduler")
@Profile("topic-service")
public class TopicEnableDisableEventsController {

    @GetMapping("/start")
    public String startSchedule() {
        return "OK";
    }

    @GetMapping("/stop")
    public String stopSchedule() {
        return "OK";
    }
}

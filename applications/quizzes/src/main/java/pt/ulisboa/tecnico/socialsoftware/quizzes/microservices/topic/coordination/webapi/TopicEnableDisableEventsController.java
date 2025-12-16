package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.webapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/scheduler")
@Profile("topic-service")
public class TopicEnableDisableEventsController {
    private static final Logger logger = LoggerFactory.getLogger(TopicEnableDisableEventsController.class);

    @GetMapping("/start")
    public String startSchedule() {
        logger.info("TopicEnableDisableEventsController: startSchedule() called");
        return "OK";
    }

    @GetMapping("/stop")
    public String stopSchedule() {
        logger.info("TopicEnableDisableEventsController: stopSchedule() called");
        return "OK";
    }
}

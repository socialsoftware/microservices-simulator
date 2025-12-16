package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.coordination.webapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/scheduler")
@Profile("user-service")
public class UserEnableDisableEventsController {
    private static final Logger logger = LoggerFactory.getLogger(UserEnableDisableEventsController.class);

    @GetMapping("/start")
    public String startSchedule() {
        logger.info("UserEnableDisableEventsController: startSchedule() called");
        return "OK";
    }

    @GetMapping("/stop")
    public String stopSchedule() {
        logger.info("UserEnableDisableEventsController: stopSchedule() called");
        return "OK";
    }
}

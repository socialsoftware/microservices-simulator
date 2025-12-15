package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.coordination.webapi;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/scheduler")
@Profile("user-service")
public class UserEnableDisableEventsController {

    @GetMapping("/start")
    public String startSchedule() {
        return "OK";
    }

    @GetMapping("/stop")
    public String stopSchedule() {
        return "OK";
    }
}

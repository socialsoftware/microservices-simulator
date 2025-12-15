package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.webapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.events.handling.QuizAnswerEventHandling;

@RestController
@RequestMapping("/scheduler")
@Profile("answer-service")
public class AnswerEnableDisableEventsController {
    private static final String SCHEDULED_TASKS = "scheduledTasks";

    @Autowired
    private ScheduledAnnotationBeanPostProcessor postProcessor;

    @Autowired
    private QuizAnswerEventHandling eventHandling;

    @GetMapping("/start")
    public String startSchedule() {
        postProcessor.postProcessAfterInitialization(eventHandling, SCHEDULED_TASKS);
        return "OK";
    }

    @GetMapping("/stop")
    public String stopSchedule() {
        postProcessor.postProcessBeforeDestruction(eventHandling, SCHEDULED_TASKS);
        return "OK";
    }
}

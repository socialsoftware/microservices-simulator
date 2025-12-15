package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.webapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.events.handling.QuestionEventHandling;

@RestController
@RequestMapping("/scheduler")
@Profile("question-service")
public class QuestionEnableDisableEventsController {
    private static final String SCHEDULED_TASKS = "scheduledTasks";

    @Autowired
    private ApplicationContext context;

    @Autowired
    private QuestionEventHandling eventHandling;

    @GetMapping("/start")
    public String startSchedule() {
        ScheduledAnnotationBeanPostProcessor postProcessor = context.getBean(ScheduledAnnotationBeanPostProcessor.class);
        postProcessor.postProcessAfterInitialization(eventHandling, SCHEDULED_TASKS);
        return "OK";
    }

    @GetMapping("/stop")
    public String stopSchedule() {
        ScheduledAnnotationBeanPostProcessor postProcessor = context.getBean(ScheduledAnnotationBeanPostProcessor.class);
        postProcessor.postProcessBeforeDestruction(eventHandling, SCHEDULED_TASKS);
        return "OK";
    }
}

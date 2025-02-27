package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.events.handling.QuizAnswerEventHandling;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.events.handling.QuestionEventHandling;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.events.handling.CourseExecutionEventHandling;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.events.handling.QuizEventHandling;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.events.handling.TournamentEventHandling;

@RestController
public class EnableDisableEventsController {
    private static final String SCHEDULED_TASKS = "scheduledTasks";

    @Autowired
    private ScheduledAnnotationBeanPostProcessor postProcessor;

    @Autowired
    private QuestionEventHandling questionEventHandling;

    @Autowired
    private CourseExecutionEventHandling executionEventDetection;

    @Autowired
    private QuizAnswerEventHandling quizAnswerEventHandling;

    @Autowired
    private QuizEventHandling quizEventHandling;

    @Autowired
    private TournamentEventHandling tournamentEventHandling;

    @GetMapping(value = "/scheduler/start")
    public String startSchedule() {
        postProcessor.postProcessAfterInitialization(quizAnswerEventHandling, SCHEDULED_TASKS);
        postProcessor.postProcessAfterInitialization(questionEventHandling, SCHEDULED_TASKS);
        postProcessor.postProcessAfterInitialization(quizEventHandling, SCHEDULED_TASKS);
        postProcessor.postProcessAfterInitialization(executionEventDetection, SCHEDULED_TASKS);
        postProcessor.postProcessAfterInitialization(tournamentEventHandling, SCHEDULED_TASKS);
        System.out.println("Started event detection");
        return "OK";
    }

    @GetMapping(value = "/scheduler/stop")
    public String stopSchedule() {
        postProcessor.postProcessBeforeDestruction(quizAnswerEventHandling, SCHEDULED_TASKS);
        postProcessor.postProcessBeforeDestruction(executionEventDetection, SCHEDULED_TASKS);
        postProcessor.postProcessBeforeDestruction(questionEventHandling, SCHEDULED_TASKS);
        postProcessor.postProcessBeforeDestruction(quizEventHandling, SCHEDULED_TASKS);
        postProcessor.postProcessBeforeDestruction(tournamentEventHandling, SCHEDULED_TASKS);
        System.out.println("Stopped event detection");
        return "OK";
    }
}

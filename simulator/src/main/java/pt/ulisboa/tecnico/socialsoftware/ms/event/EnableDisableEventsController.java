package pt.ulisboa.tecnico.socialsoftware.ms.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@EnableScheduling
@RequestMapping("/scheduler")
public class EnableDisableEventsController {
    private static final Logger logger = LoggerFactory.getLogger(EnableDisableEventsController.class);
    private static final String SCHEDULED_TASKS = "scheduledTasks";

    @Autowired
    private ScheduledAnnotationBeanPostProcessor postProcessor;

    @Autowired(required = false)
    private List<EventHandling> eventHandlings = Collections.emptyList();

    @GetMapping("/start")
    public String startSchedule() {
        logger.info("EnableDisableEventsController: startSchedule() called with {} event handlers", eventHandlings.size());
        for (EventHandling eventHandling : eventHandlings) {
            postProcessor.postProcessAfterInitialization(eventHandling, SCHEDULED_TASKS);
        }
        return "OK";
    }

    @GetMapping("/stop")
    public String stopSchedule() {
        logger.info("EnableDisableEventsController: stopSchedule() called with {} event handlers", eventHandlings.size());
        for (EventHandling eventHandling : eventHandlings) {
            postProcessor.postProcessBeforeDestruction(eventHandling, SCHEDULED_TASKS);
        }
        return "OK";
    }
}

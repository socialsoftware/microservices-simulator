package pt.ulisboa.tecnico.socialsoftware.blcm.user.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.blcm.causalconsistency.event.Event;
import pt.ulisboa.tecnico.socialsoftware.blcm.causalconsistency.event.EventRepository;
import pt.ulisboa.tecnico.socialsoftware.blcm.causalconsistency.event.RemoveCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.blcm.causalconsistency.unityOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.blcm.causalconsistency.unityOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.blcm.causalconsistency.event.utils.ProcessedEvents;
import pt.ulisboa.tecnico.socialsoftware.blcm.causalconsistency.event.utils.ProcessedEventsRepository;
import pt.ulisboa.tecnico.socialsoftware.blcm.user.domain.User;
import pt.ulisboa.tecnico.socialsoftware.blcm.user.repository.UserRepository;
import pt.ulisboa.tecnico.socialsoftware.blcm.user.service.UserService;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static pt.ulisboa.tecnico.socialsoftware.blcm.causalconsistency.event.EventType.REMOVE_COURSE_EXECUTION;

@Component
public class UserEventDetection {

    @Autowired
    private UnitOfWorkService unitOfWorkService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProcessedEventsRepository processedEventsRepository;

    @Scheduled(fixedDelay = 1000)
    public void detectRemoveCourseExecutionEvents() {
        /*Set<Integer> userAggregateIds = userRepository.findAll().stream().map(User::getAggregateId).collect(Collectors.toSet());

        for(Integer userAggregateId : userAggregateIds) {
            ProcessedEvents userProcessedEvents = getUserProcessedEvents(REMOVE_COURSE_EXECUTION, userAggregateId);
            List<Event> events = getDomainEvents(REMOVE_COURSE_EXECUTION, userProcessedEvents);

            processRemoveCourseExecutionEvents(userAggregateId, userProcessedEvents, events);
            processedEventsRepository.save(userProcessedEvents);
        }
    }

    private void processRemoveCourseExecutionEvents(Integer userAggregateId, ProcessedEvents userProcessedEvents, List<Event> events) {
        Set<RemoveCourseExecutionEvent> anonymizeUserEvents = events.stream().map(e -> (RemoveCourseExecutionEvent) e).collect(Collectors.toSet());
        for(RemoveCourseExecutionEvent e : anonymizeUserEvents) {
            Set<Integer> userIdsByCourseExecution = userRepository.findAllAggregateIdsByCourseExecution(e.getAggregateId());
            if(!userIdsByCourseExecution.contains(userAggregateId)) {
                continue;
            }
            System.out.printf("Processing remove course execution %d event for user %d\n", e.getAggregateId(), userAggregateId);
            UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork();
            User updatedUser = userService.removeCourseExecutionsFromUser(userAggregateId, List.of(e.getAggregateId()), unitOfWork);
            if(updatedUser != null) {
                updatedUser.addProcessedEvent(e.getType(), e.getAggregateVersion());
                unitOfWorkService.commit(unitOfWork);
            }
            unitOfWorkService.commit(unitOfWork);
            userProcessedEvents.addProcessedEventVersion(e.getId());
        }
    }

    private List<Event> getDomainEvents(String eventType, ProcessedEvents userProcessedEvents) {
        return eventRepository.findAll()
                .stream()
                .filter(e -> eventType.equals(e.getType()))
                .filter(e -> !(userProcessedEvents.containsEventVersion(e.getId())))
                .distinct()
                .sorted(Comparator.comparing(Event::getTs).reversed())
                .collect(Collectors.toList());
    }

    private ProcessedEvents getUserProcessedEvents(String eventType, Integer userAggregateId) {
        return processedEventsRepository.findAll().stream()
                .filter(pe -> userAggregateId.equals(pe.getAggregateId()))
                .filter(pe -> eventType.equals(pe.getEventType()))
                .findFirst()
                .orElse(new ProcessedEvents(eventType, userAggregateId));*/
    }
}

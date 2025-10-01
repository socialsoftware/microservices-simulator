package pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.aggregate.*;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.service.*;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing.CourseEventProcessing;
import java.util.Set;
import java.util.stream.Collectors;


public abstract class CourseEventHandler extends EventHandler {
private CourseRepository courseRepository;
protected CourseEventProcessing courseEventProcessing;

public CourseEventHandler(CourseRepository courseRepository,
CourseEventProcessing courseEventProcessing) {
this.courseRepository = courseRepository;
this.courseEventProcessing = courseEventProcessing;
}

public Set<Integer> getAggregateIds() {
    return
    courseRepository.findAll().stream().map(Course::getAggregateId).collect(Collectors.toSet());
    }
    }
package pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.answers.events.CourseDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.Topic;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.TopicFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;

@Service
public class TopicEventProcessing {
    @Autowired
    private TopicService topicService;

    @Autowired
    private TopicFactory topicFactory;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public TopicEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }

    public void processCourseDeletedEvent(Integer aggregateId, CourseDeletedEvent courseDeletedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        Topic oldTopic = (Topic) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        Topic newTopic = topicFactory.createTopicFromExisting(oldTopic);
        newTopic.setState(Aggregate.AggregateState.INACTIVE);
        unitOfWorkService.registerChanged(newTopic, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }
}
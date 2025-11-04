package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.events.publish.DeleteQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.events.publish.UpdateQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class QuestionService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    private final QuestionRepository questionRepository;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    private final QuestionTransactionalService questionTransactionalService;

    @Autowired
    private QuestionFactory questionFactory;

    public QuestionService(UnitOfWorkService unitOfWorkService, QuestionRepository questionRepository) {
        this.unitOfWorkService = unitOfWorkService;
        this.questionRepository = questionRepository;
        this.questionTransactionalService = new QuestionTransactionalService();
    }

    @Retryable(retryFor = {
            TransientDataAccessException.class,
            SQLException.class }, maxAttempts = 5, backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000))
    public QuestionDto getQuestionById(Integer aggregateId, UnitOfWork unitOfWork) {
        return questionTransactionalService.getQuestionByIdTransactional(aggregateId, unitOfWork, unitOfWorkService,
                questionFactory);
    }

    @Retryable(retryFor = {
            TransientDataAccessException.class,
            SQLException.class }, maxAttempts = 5, backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000))
    public List<QuestionDto> findQuestionsByCourseAggregateId(Integer courseAggregateId, UnitOfWork unitOfWork) {
        return questionTransactionalService.findQuestionsByCourseAggregateIdTransactional(courseAggregateId, unitOfWork,
                questionRepository, unitOfWorkService);
    }

    @Retryable(retryFor = {
            TransientDataAccessException.class,
            SQLException.class }, maxAttempts = 5, backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000))
    public QuestionDto createQuestion(QuestionCourse course, QuestionDto questionDto, List<TopicDto> topics,
            UnitOfWork unitOfWork) {
        return questionTransactionalService.createQuestionTransactional(course, questionDto, topics, unitOfWork,
                aggregateIdGeneratorService, questionFactory, unitOfWorkService);
    }

    @Retryable(retryFor = {
            TransientDataAccessException.class,
            SQLException.class }, maxAttempts = 5, backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000))
    public void updateQuestion(QuestionDto questionDto, UnitOfWork unitOfWork) {
        questionTransactionalService.updateQuestionTransactional(questionDto, unitOfWork, unitOfWorkService,
                questionFactory);
    }

    @Retryable(retryFor = {
            TransientDataAccessException.class,
            SQLException.class }, maxAttempts = 5, backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000))
    public void removeQuestion(Integer courseAggregateId, UnitOfWork unitOfWork) {
        questionTransactionalService.removeQuestionTransactional(courseAggregateId, unitOfWork, unitOfWorkService,
                questionFactory);
    }

    @Retryable(retryFor = {
            TransientDataAccessException.class,
            SQLException.class }, maxAttempts = 5, backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000))
    public void updateQuestionTopics(Integer courseAggregateId, Set<QuestionTopic> topics, UnitOfWork unitOfWork) {
        questionTransactionalService.updateQuestionTopicsTransactional(courseAggregateId, topics, unitOfWork,
                unitOfWorkService, questionFactory);
    }

    @Retryable(retryFor = {
            TransientDataAccessException.class,
            SQLException.class }, maxAttempts = 5, backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000))
    public List<QuestionDto> findQuestionsByTopicIds(List<Integer> topicIds, UnitOfWork unitOfWork) {
        return questionTransactionalService.findQuestionsByTopicIdsTransactional(topicIds, unitOfWork,
                questionRepository, unitOfWorkService);
    }

    /************************************************
     * EVENT PROCESSING
     ************************************************/

    @Retryable(retryFor = {
            TransientDataAccessException.class,
            SQLException.class }, maxAttempts = 5, backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000))
    public QuestionDto updateTopic(Integer questionAggregateId, Integer topicAggregateId, String topicName,
            Integer aggregateVersion, UnitOfWork unitOfWork) {
        return questionTransactionalService.updateTopicTransactional(questionAggregateId, topicAggregateId, topicName,
                aggregateVersion, unitOfWork, unitOfWorkService, questionFactory);
    }

    @Retryable(retryFor = {
            TransientDataAccessException.class,
            SQLException.class }, maxAttempts = 5, backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000))
    public QuestionDto removeTopic(Integer questionAggregateId, Integer topicAggregateId, Integer aggregateVersion,
            UnitOfWork unitOfWork) {
        return questionTransactionalService.removeTopicTransactional(questionAggregateId, topicAggregateId,
                aggregateVersion, unitOfWork, unitOfWorkService, questionFactory);
    }

}

@Service
class QuestionTransactionalService {

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public QuestionDto getQuestionByIdTransactional(Integer aggregateId, UnitOfWork unitOfWork,
            UnitOfWorkService<UnitOfWork> unitOfWorkService, QuestionFactory questionFactory) {
        return questionFactory
                .createQuestionDto((Question) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<QuestionDto> findQuestionsByCourseAggregateIdTransactional(Integer courseAggregateId,
            UnitOfWork unitOfWork,
            QuestionRepository questionRepository, UnitOfWorkService<UnitOfWork> unitOfWorkService) {
        return questionRepository.findAll().stream()
                .filter(q -> q.getQuestionCourse().getCourseAggregateId() == courseAggregateId)
                .map(Question::getAggregateId)
                .distinct()
                .map(id -> (Question) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .map(QuestionDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public QuestionDto createQuestionTransactional(QuestionCourse course, QuestionDto questionDto,
            List<TopicDto> topics,
            UnitOfWork unitOfWork, AggregateIdGeneratorService aggregateIdGeneratorService,
            QuestionFactory questionFactory, UnitOfWorkService<UnitOfWork> unitOfWorkService) {
        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();

        List<QuestionTopic> questionTopics = topics.stream()
                .map(QuestionTopic::new)
                .collect(Collectors.toList());

        Question question = questionFactory.createQuestion(aggregateId, course, questionDto, questionTopics);
        unitOfWorkService.registerChanged(question, unitOfWork);
        return questionFactory.createQuestionDto(question);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void updateQuestionTransactional(QuestionDto questionDto, UnitOfWork unitOfWork,
            UnitOfWorkService<UnitOfWork> unitOfWorkService, QuestionFactory questionFactory) {
        Question oldQuestion = (Question) unitOfWorkService.aggregateLoadAndRegisterRead(questionDto.getAggregateId(),
                unitOfWork);
        Question newQuestion = questionFactory.createQuestionFromExisting(oldQuestion);
        newQuestion.update(questionDto);
        unitOfWorkService.registerChanged(newQuestion, unitOfWork);
        unitOfWorkService.registerEvent(
                new UpdateQuestionEvent(newQuestion.getAggregateId(), newQuestion.getTitle(), newQuestion.getContent()),
                unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void removeQuestionTransactional(Integer courseAggregateId, UnitOfWork unitOfWork,
            UnitOfWorkService<UnitOfWork> unitOfWorkService, QuestionFactory questionFactory) {
        Question oldQuestion = (Question) unitOfWorkService.aggregateLoadAndRegisterRead(courseAggregateId, unitOfWork);
        Question newQuestion = questionFactory.createQuestionFromExisting(oldQuestion);
        newQuestion.remove();
        unitOfWorkService.registerChanged(newQuestion, unitOfWork);
        unitOfWorkService.registerEvent(new DeleteQuestionEvent(newQuestion.getAggregateId()), unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void updateQuestionTopicsTransactional(Integer courseAggregateId, Set<QuestionTopic> topics,
            UnitOfWork unitOfWork,
            UnitOfWorkService<UnitOfWork> unitOfWorkService, QuestionFactory questionFactory) {
        Question oldQuestion = (Question) unitOfWorkService.aggregateLoadAndRegisterRead(courseAggregateId, unitOfWork);
        Question newQuestion = questionFactory.createQuestionFromExisting(oldQuestion);
        newQuestion.setQuestionTopics(topics);
        unitOfWorkService.registerChanged(newQuestion, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<QuestionDto> findQuestionsByTopicIdsTransactional(List<Integer> topicIds, UnitOfWork unitOfWork,
            QuestionRepository questionRepository, UnitOfWorkService<UnitOfWork> unitOfWorkService) {
        Set<Integer> questionAggregateIds = questionRepository.findAll().stream()
                .filter(q -> {
                    for (QuestionTopic qt : q.getQuestionTopics()) {
                        if (topicIds.contains(qt.getTopicAggregateId())) {
                            return true;
                        }
                    }
                    return false;
                })
                .map(Question::getAggregateId)
                .collect(Collectors.toSet());
        return questionAggregateIds.stream()
                .map(id -> (Question) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .map(QuestionDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public QuestionDto updateTopicTransactional(Integer questionAggregateId, Integer topicAggregateId, String topicName,
            Integer aggregateVersion, UnitOfWork unitOfWork, UnitOfWorkService<UnitOfWork> unitOfWorkService,
            QuestionFactory questionFactory) {
        Question oldQuestion = (Question) unitOfWorkService.aggregateLoadAndRegisterRead(questionAggregateId,
                unitOfWork);
        Question newQuestion = questionFactory.createQuestionFromExisting(oldQuestion);

        QuestionTopic questionTopic = newQuestion.findTopic(topicAggregateId);
        /*
         * if(questionTopic != null &&
         * questionTopic.getAggregateId().equals(topicAggregateId) &&
         * questionTopic.getVersion() >= aggregateVersion) {
         * return null;
         * }
         */
        questionTopic.setTopicName(topicName);
        questionTopic.setTopicVersion(aggregateVersion);
        unitOfWorkService.registerChanged(newQuestion, unitOfWork);
        return new QuestionDto(newQuestion);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public QuestionDto removeTopicTransactional(Integer questionAggregateId, Integer topicAggregateId,
            Integer aggregateVersion,
            UnitOfWork unitOfWork, UnitOfWorkService<UnitOfWork> unitOfWorkService, QuestionFactory questionFactory) {
        Question oldQuestion = (Question) unitOfWorkService.aggregateLoadAndRegisterRead(questionAggregateId,
                unitOfWork);
        Question newQuestion = questionFactory.createQuestionFromExisting(oldQuestion);

        QuestionTopic questionTopic = newQuestion.findTopic(topicAggregateId);
        if (questionTopic != null && questionTopic.getTopicAggregateId().equals(topicAggregateId)
                && questionTopic.getTopicVersion() >= aggregateVersion) {
            return null;
        }
        if (questionTopic != null) {
            questionTopic.setState(Aggregate.AggregateState.INACTIVE);
        }
        unitOfWorkService.registerChanged(newQuestion, unitOfWork);
        return new QuestionDto(newQuestion);
    }
}

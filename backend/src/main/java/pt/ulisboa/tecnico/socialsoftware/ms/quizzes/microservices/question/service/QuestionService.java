package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.Question;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionCourse;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionTopic;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.events.publish.DeleteQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.events.publish.UpdateQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicDto;

@Service
public class QuestionService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;
    
    private final QuestionRepository questionRepository;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    @Autowired
    private QuestionFactory questionFactory;
    
    public QuestionService(UnitOfWorkService unitOfWorkService, QuestionRepository questionRepository) {
        this.unitOfWorkService = unitOfWorkService;
        this.questionRepository = questionRepository;
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public QuestionDto getQuestionById(Integer aggregateId, UnitOfWork unitOfWork) {
        return questionFactory.createQuestionDto((Question) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork));
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<QuestionDto> findQuestionsByCourseAggregateId(Integer courseAggregateId, UnitOfWork unitOfWork) {
        return questionRepository.findAll().stream()
                .filter(q -> q.getQuestionCourse().getCourseAggregateId() == courseAggregateId)
                .map(Question::getAggregateId)
                .distinct()
                .map(id -> (Question) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .map(QuestionDto::new)
                .collect(Collectors.toList());
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public QuestionDto createQuestion(QuestionCourse course, QuestionDto questionDto, List<TopicDto> topics, UnitOfWork unitOfWork) {
        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();

        List<QuestionTopic> questionTopics = topics.stream()
                .map(QuestionTopic::new)
                .collect(Collectors.toList());

        Question question = questionFactory.createQuestion(aggregateId, course, questionDto, questionTopics);
        unitOfWork.registerChanged(question);
        return questionFactory.createQuestionDto(question);
    }

    private void checkInput(QuestionCourse course, QuestionDto questionDto) {
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void updateQuestion(QuestionDto questionDto, UnitOfWork unitOfWork) {
        Question oldQuestion = (Question) unitOfWorkService.aggregateLoadAndRegisterRead(questionDto.getAggregateId(), unitOfWork);
        Question newQuestion = questionFactory.createQuestionFromExisting(oldQuestion);
        newQuestion.update(questionDto);
        unitOfWork.registerChanged(newQuestion);
        unitOfWork.addEvent(new UpdateQuestionEvent(newQuestion.getAggregateId(), newQuestion.getTitle(), newQuestion.getContent()));
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void removeQuestion(Integer courseAggregateId, UnitOfWork unitOfWork) {
        Question oldQuestion = (Question) unitOfWorkService.aggregateLoadAndRegisterRead(courseAggregateId, unitOfWork);
        Question newQuestion = questionFactory.createQuestionFromExisting(oldQuestion);
        newQuestion.remove();
        unitOfWork.registerChanged(newQuestion);
        unitOfWork.addEvent(new DeleteQuestionEvent(newQuestion.getAggregateId()));
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void updateQuestionTopics(Integer courseAggregateId, Set<QuestionTopic> topics, UnitOfWork unitOfWork) {
        Question oldQuestion = (Question) unitOfWorkService.aggregateLoadAndRegisterRead(courseAggregateId, unitOfWork);
        Question newQuestion = questionFactory.createQuestionFromExisting(oldQuestion);
        newQuestion.setQuestionTopics(topics);
        unitOfWork.registerChanged(newQuestion);
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<QuestionDto> findQuestionsByTopicIds(List<Integer> topicIds, UnitOfWork unitOfWork) {
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

    /************************************************ EVENT PROCESSING ************************************************/

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Question updateTopic(Integer questionAggregateId, Integer topicAggregateId, String topicName, Integer aggregateVersion, UnitOfWork unitOfWork) {
        Question oldQuestion = (Question) unitOfWorkService.aggregateLoadAndRegisterRead(questionAggregateId, unitOfWork);
        Question newQuestion = questionFactory.createQuestionFromExisting(oldQuestion);

        QuestionTopic questionTopic = newQuestion.findTopic(topicAggregateId);
        /*
        if(questionTopic != null && questionTopic.getAggregateId().equals(topicAggregateId) && questionTopic.getVersion() >= aggregateVersion) {
            return null;
        }
        */
        questionTopic.setTopicName(topicName);
        questionTopic.setTopicVersion(aggregateVersion);
        unitOfWork.registerChanged(newQuestion);
        return newQuestion;
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Question removeTopic(Integer questionAggregateId, Integer topicAggregateId, Integer aggregateVersion, UnitOfWork unitOfWork) {
        Question oldQuestion = (Question) unitOfWorkService.aggregateLoadAndRegisterRead(questionAggregateId, unitOfWork);
        Question newQuestion = questionFactory.createQuestionFromExisting(oldQuestion);

        QuestionTopic questionTopic = newQuestion.findTopic(topicAggregateId);
        if(questionTopic != null && questionTopic.getTopicAggregateId().equals(topicAggregateId) && questionTopic.getTopicVersion() >= aggregateVersion) {
            return null;
        }
        if(questionTopic != null) {
            questionTopic.setState(Aggregate.AggregateState.INACTIVE);
        }
        unitOfWork.registerChanged(newQuestion);
        return newQuestion;
    }


}

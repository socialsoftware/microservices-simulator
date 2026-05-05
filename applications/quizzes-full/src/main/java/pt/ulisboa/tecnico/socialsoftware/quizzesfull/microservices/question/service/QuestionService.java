package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.DeleteQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.UpdateQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.Option;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.Question;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.QuestionCourse;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.QuestionCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.QuestionFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.QuestionTopic;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class QuestionService {

    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private QuestionFactory questionFactory;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;
    private final QuestionCustomRepository questionRepository;

    public QuestionService(UnitOfWorkService unitOfWorkService, QuestionCustomRepository questionRepository) {
        this.unitOfWorkService = unitOfWorkService;
        this.questionRepository = questionRepository;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public QuestionDto getQuestionById(Integer questionAggregateId, UnitOfWork unitOfWork) {
        return questionFactory.createQuestionDto(
                (Question) unitOfWorkService.aggregateLoadAndRegisterRead(questionAggregateId, unitOfWork));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public QuestionDto createQuestion(String title, String content, QuestionCourse questionCourse,
                                      Set<Option> options, Set<QuestionTopic> topics,
                                      UnitOfWork unitOfWork) {
        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        Question question = questionFactory.createQuestion(aggregateId, title, content, questionCourse, options, topics);
        unitOfWorkService.registerChanged(question, unitOfWork);
        return questionFactory.createQuestionDto(question);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void updateQuestion(Integer questionAggregateId, String title, String content,
                               Set<QuestionTopic> topics, UnitOfWork unitOfWork) {
        Question oldQuestion = (Question) unitOfWorkService.aggregateLoadAndRegisterRead(
                questionAggregateId, unitOfWork);
        Question newQuestion = questionFactory.createQuestionCopy(oldQuestion);
        newQuestion.setTitle(title);
        newQuestion.setContent(content);
        newQuestion.setTopics(topics);
        unitOfWorkService.registerChanged(newQuestion, unitOfWork);
        unitOfWorkService.registerEvent(
                new UpdateQuestionEvent(questionAggregateId, title, content), unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<QuestionDto> getQuestionsByCourseExecutionId(Integer courseAggregateId, UnitOfWork unitOfWork) {
        return questionRepository.findQuestionIdsByCourseId(courseAggregateId).stream()
                .map(id -> (Question) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .map(q -> questionFactory.createQuestionDto(q))
                .collect(Collectors.toList());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void updateTopicNameInQuestion(Integer questionAggregateId, Integer topicAggregateId, String topicName, UnitOfWork unitOfWork) {
        Question oldQuestion = (Question) unitOfWorkService.aggregateLoadAndRegisterRead(
                questionAggregateId, unitOfWork);
        Question newQuestion = questionFactory.createQuestionCopy(oldQuestion);
        for (QuestionTopic topic : newQuestion.getTopics()) {
            if (topic.getTopicAggregateId().equals(topicAggregateId)) {
                topic.setTopicName(topicName);
                break;
            }
        }
        unitOfWorkService.registerChanged(newQuestion, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void removeTopicFromQuestion(Integer questionAggregateId, Integer topicAggregateId, UnitOfWork unitOfWork) {
        Question oldQuestion = (Question) unitOfWorkService.aggregateLoadAndRegisterRead(
                questionAggregateId, unitOfWork);
        Question newQuestion = questionFactory.createQuestionCopy(oldQuestion);
        newQuestion.getTopics().removeIf(t -> t.getTopicAggregateId().equals(topicAggregateId));
        unitOfWorkService.registerChanged(newQuestion, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteQuestion(Integer questionAggregateId, UnitOfWork unitOfWork) {
        Question oldQuestion = (Question) unitOfWorkService.aggregateLoadAndRegisterRead(
                questionAggregateId, unitOfWork);
        Question newQuestion = questionFactory.createQuestionCopy(oldQuestion);
        Integer courseAggregateId = newQuestion.getQuestionCourse().getCourseAggregateId();
        newQuestion.remove();
        unitOfWorkService.registerChanged(newQuestion, unitOfWork);
        unitOfWorkService.registerEvent(
                new DeleteQuestionEvent(questionAggregateId, courseAggregateId), unitOfWork);
    }
}

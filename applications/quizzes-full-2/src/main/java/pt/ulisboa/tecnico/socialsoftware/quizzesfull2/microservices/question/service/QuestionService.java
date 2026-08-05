package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.DeleteQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.UpdateQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.Option;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.Question;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.QuestionCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.QuestionFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.QuestionTopic;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.QuestionTopicDto;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuestionService {
    private final QuestionCustomRepository questionCustomRepository;
    private final QuestionFactory questionFactory;
    private final UnitOfWorkService unitOfWorkService;
    private final AggregateIdGeneratorService aggregateIdGeneratorService;

    public QuestionService(QuestionCustomRepository questionCustomRepository,
                           QuestionFactory questionFactory,
                           UnitOfWorkService unitOfWorkService,
                           AggregateIdGeneratorService aggregateIdGeneratorService) {
        this.questionCustomRepository = questionCustomRepository;
        this.questionFactory = questionFactory;
        this.unitOfWorkService = unitOfWorkService;
        this.aggregateIdGeneratorService = aggregateIdGeneratorService;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public QuestionDto getQuestionById(Integer questionAggregateId, UnitOfWork unitOfWork) {
        return questionFactory.createQuestionDto(
                (Question) unitOfWorkService.aggregateLoadAndRegisterRead(questionAggregateId, unitOfWork));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<QuestionDto> getQuestionsByCourse(Integer courseAggregateId, UnitOfWork unitOfWork) {
        return questionCustomRepository.findQuestionIdsByCourse(courseAggregateId).stream()
                .map(questionAggregateId -> questionFactory.createQuestionDto(
                        (Question) unitOfWorkService.aggregateLoadAndRegisterRead(questionAggregateId, unitOfWork)))
                .collect(Collectors.toList());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public QuestionDto createQuestion(QuestionDto questionDto, List<QuestionTopicDto> topics,
                                      UnitOfWork unitOfWork) {
        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        Question question = questionFactory.createQuestion(aggregateId, questionDto.getCourseAggregateId(),
                questionDto.getTitle(), questionDto.getContent(), DateHandler.now());

        questionDto.getOptions().forEach(optionDto -> question.addOption(new Option(optionDto.getSequence(),
                optionDto.getOptionKey(), optionDto.getContent(), optionDto.isCorrect())));
        topics.forEach(topicDto -> question.addTopic(toQuestionTopic(topicDto)));

        unitOfWorkService.registerChanged(question, unitOfWork);
        return questionFactory.createQuestionDto(question);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void updateQuestion(Integer questionAggregateId, String title, String content,
                               List<QuestionTopicDto> topics, UnitOfWork unitOfWork) {
        Question oldQuestion = (Question) unitOfWorkService.aggregateLoadAndRegisterRead(
                questionAggregateId, unitOfWork);
        Question newQuestion = questionFactory.createQuestionCopy(oldQuestion);

        newQuestion.setTitle(title);
        newQuestion.setContent(content);
        newQuestion.setTopics(topics.stream().map(this::toQuestionTopic).collect(Collectors.toList()));

        unitOfWorkService.registerChanged(newQuestion, unitOfWork);
        unitOfWorkService.registerEvent(new UpdateQuestionEvent(newQuestion.getAggregateId(),
                newQuestion.getTitle(), newQuestion.getContent()), unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteQuestion(Integer questionAggregateId, UnitOfWork unitOfWork) {
        Question oldQuestion = (Question) unitOfWorkService.aggregateLoadAndRegisterRead(
                questionAggregateId, unitOfWork);
        Question newQuestion = questionFactory.createQuestionCopy(oldQuestion);

        newQuestion.remove();

        unitOfWorkService.registerChanged(newQuestion, unitOfWork);
        unitOfWorkService.registerEvent(new DeleteQuestionEvent(newQuestion.getAggregateId(),
                newQuestion.getCourseAggregateId()), unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void setTopicName(Integer questionAggregateId, Integer topicAggregateId, String topicName,
                             Long topicVersion, UnitOfWork unitOfWork) {
        Question oldQuestion = (Question) unitOfWorkService.aggregateLoadAndRegisterRead(
                questionAggregateId, unitOfWork);
        Question newQuestion = questionFactory.createQuestionCopy(oldQuestion);

        QuestionTopic topic = findTopic(newQuestion, topicAggregateId);
        if (topic == null) {
            return;
        }
        topic.setTopicName(topicName);
        topic.setTopicVersion(topicVersion);

        newQuestion.verifyInvariants();
        unitOfWorkService.registerChanged(newQuestion, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void removeDeletedTopic(Integer questionAggregateId, Integer topicAggregateId, UnitOfWork unitOfWork) {
        Question oldQuestion = (Question) unitOfWorkService.aggregateLoadAndRegisterRead(
                questionAggregateId, unitOfWork);
        Question newQuestion = questionFactory.createQuestionCopy(oldQuestion);

        if (findTopic(newQuestion, topicAggregateId) == null) {
            return;
        }
        newQuestion.removeTopic(topicAggregateId);

        newQuestion.verifyInvariants();
        unitOfWorkService.registerChanged(newQuestion, unitOfWork);
    }

    private static QuestionTopic findTopic(Question question, Integer topicAggregateId) {
        return question.getTopics().stream()
                .filter(topic -> topicAggregateId.equals(topic.getTopicAggregateId()))
                .findFirst()
                .orElse(null);
    }

    private QuestionTopic toQuestionTopic(QuestionTopicDto topicDto) {
        return new QuestionTopic(topicDto.getTopicAggregateId(), topicDto.getTopicName(),
                topicDto.getTopicVersion(), topicDto.getCourseAggregateId());
    }
}

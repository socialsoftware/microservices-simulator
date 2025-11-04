package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.service;

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
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesException;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.events.publish.InvalidateQuizEvent;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizType.GENERATED;
import static pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizType.IN_CLASS;

@Service
public class QuizService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;
    @Autowired
    private QuestionService questionService;
    @Autowired
    private CourseExecutionService courseExecutionService;

    private final QuizRepository quizRepository;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    private final QuizTransactionalService quizTransactionalService;

    @Autowired
    private QuizFactory quizFactory;

    public QuizService(UnitOfWorkService unitOfWorkService, QuizRepository quizRepository) {
        this.unitOfWorkService = unitOfWorkService;
        this.quizRepository = quizRepository;
        this.quizTransactionalService = new QuizTransactionalService();
    }

    @Retryable(retryFor = {
            TransientDataAccessException.class,
            SQLException.class }, maxAttempts = 5, backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000))
    public QuizDto getQuizById(Integer aggregateId, UnitOfWork unitOfWork) {
        return quizTransactionalService.getQuizByIdTransactional(aggregateId, unitOfWork, unitOfWorkService,
                quizFactory);
    }

    // intended for requests from local functionalities

    @Retryable(retryFor = {
            TransientDataAccessException.class,
            SQLException.class }, maxAttempts = 5, backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000))
    public QuizDto generateQuiz(Integer courseExecutionAggregateId, QuizDto quizDto, List<Integer> topicIds,
            Integer numberOfQuestions, UnitOfWork unitOfWork) {
        return quizTransactionalService.generateQuizTransactional(courseExecutionAggregateId, quizDto, topicIds,
                numberOfQuestions, unitOfWork, aggregateIdGeneratorService, courseExecutionService, questionService,
                quizFactory, unitOfWorkService);
    }

    @Retryable(retryFor = {
            TransientDataAccessException.class,
            SQLException.class }, maxAttempts = 5, backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000))
    public QuizDto startTournamentQuiz(Integer userAggregateId, Integer quizAggregateId, UnitOfWork unitOfWork) {
        return quizTransactionalService.startTournamentQuizTransactional(userAggregateId, quizAggregateId, unitOfWork,
                unitOfWorkService, questionService, quizFactory);
    }

    @Retryable(retryFor = {
            TransientDataAccessException.class,
            SQLException.class }, maxAttempts = 5, backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000))
    public QuizDto createQuiz(QuizCourseExecution quizCourseExecution, Set<QuestionDto> questions, QuizDto quizDto,
            UnitOfWork unitOfWork) {
        return quizTransactionalService.createQuizTransactional(quizCourseExecution, questions, quizDto, unitOfWork,
                aggregateIdGeneratorService, quizFactory, unitOfWorkService);
    }

    @Retryable(retryFor = {
            TransientDataAccessException.class,
            SQLException.class }, maxAttempts = 5, backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000))
    public QuizDto updateGeneratedQuiz(QuizDto quizDto, Set<Integer> topicsAggregateIds, Integer numberOfQuestions,
            UnitOfWork unitOfWork) {
        return quizTransactionalService.updateGeneratedQuizTransactional(quizDto, topicsAggregateIds, numberOfQuestions,
                unitOfWork, unitOfWorkService, questionService, quizFactory);
    }

    @Retryable(retryFor = {
            TransientDataAccessException.class,
            SQLException.class }, maxAttempts = 5, backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000))
    public QuizDto updateQuiz(QuizDto quizDto, Set<QuizQuestion> quizQuestions, UnitOfWork unitOfWork) {
        return quizTransactionalService.updateQuizTransactional(quizDto, quizQuestions, unitOfWork, unitOfWorkService,
                quizFactory);
    }

    @Retryable(retryFor = {
            TransientDataAccessException.class,
            SQLException.class }, maxAttempts = 5, backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000))
    public List<QuizDto> getAvailableQuizzes(Integer courseExecutionAggregateId, UnitOfWork unitOfWork) {
        return quizTransactionalService.getAvailableQuizzesTransactional(courseExecutionAggregateId, unitOfWork,
                quizRepository, unitOfWorkService);
    }

    /************************************************
     * EVENT PROCESSING
     ************************************************/

    @Retryable(retryFor = {
            TransientDataAccessException.class,
            SQLException.class }, maxAttempts = 5, backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000))
    public QuizDto removeCourseExecution(Integer quizAggregateId, Integer courseExecutionId, Integer aggregateVersion,
            UnitOfWork unitOfWork) {
        return quizTransactionalService.removeCourseExecutionTransactional(quizAggregateId, courseExecutionId,
                aggregateVersion, unitOfWork, unitOfWorkService, quizFactory);
    }

    @Retryable(retryFor = {
            TransientDataAccessException.class,
            SQLException.class }, maxAttempts = 5, backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000))
    public void updateQuestion(Integer quizAggregateId, Integer questionAggregateId, String title, String content,
            Integer aggregateVersion, UnitOfWork unitOfWork) {
        quizTransactionalService.updateQuestionTransactional(quizAggregateId, questionAggregateId, title, content,
                aggregateVersion, unitOfWork, unitOfWorkService, quizFactory);
    }

    @Retryable(retryFor = {
            TransientDataAccessException.class,
            SQLException.class }, maxAttempts = 5, backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000))
    public void removeQuizQuestion(Integer quizAggregateId, Integer questionAggregateId, UnitOfWork unitOfWork) {
        quizTransactionalService.removeQuizQuestionTransactional(quizAggregateId, questionAggregateId, unitOfWork,
                unitOfWorkService, quizFactory);
    }

    @Retryable(retryFor = {
            TransientDataAccessException.class,
            SQLException.class }, maxAttempts = 5, backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000))
    public void removeQuiz(Integer quizAggregateId, UnitOfWork unitOfWork) {
        quizTransactionalService.removeQuizTransactional(quizAggregateId, unitOfWork, unitOfWorkService, quizFactory);
    }
}

@Service
class QuizTransactionalService {

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public QuizDto getQuizByIdTransactional(Integer aggregateId, UnitOfWork unitOfWork,
            UnitOfWorkService<UnitOfWork> unitOfWorkService, QuizFactory quizFactory) {
        return quizFactory
                .createQuizDto((Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public QuizDto generateQuizTransactional(Integer courseExecutionAggregateId, QuizDto quizDto,
            List<Integer> topicIds, Integer numberOfQuestions, UnitOfWork unitOfWork,
            AggregateIdGeneratorService aggregateIdGeneratorService, CourseExecutionService courseExecutionService,
            QuestionService questionService, QuizFactory quizFactory,
            UnitOfWorkService<UnitOfWork> unitOfWorkService) {
        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        System.out.println("QUIZ AGGREGATE ID: " + aggregateId);

        QuizCourseExecution quizCourseExecution = new QuizCourseExecution(
                courseExecutionService.getCourseExecutionById(courseExecutionAggregateId, unitOfWork));

        List<QuestionDto> questionDtos = questionService.findQuestionsByTopicIds(topicIds, unitOfWork);

        if (questionDtos.size() < numberOfQuestions) {
            throw new QuizzesException(QuizzesErrorMessage.NOT_ENOUGH_QUESTIONS);
        }

        Set<Integer> questionPositions = new HashSet<>();
        while (questionPositions.size() < numberOfQuestions) {
            questionPositions.add(ThreadLocalRandom.current().nextInt(0, questionDtos.size()));
        }

        Set<QuizQuestion> quizQuestions = questionPositions.stream()
                .map(pos -> questionDtos.get(pos))
                .map(QuizQuestion::new)
                .collect(Collectors.toSet());

        Quiz quiz = quizFactory.createQuiz(aggregateId, quizCourseExecution, quizQuestions, quizDto, GENERATED);
        quiz.setTitle("Generated Quiz Title");
        unitOfWorkService.registerChanged(quiz, unitOfWork);
        return quizFactory.createQuizDto(quiz);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public QuizDto startTournamentQuizTransactional(Integer userAggregateId, Integer quizAggregateId,
            UnitOfWork unitOfWork,
            UnitOfWorkService<UnitOfWork> unitOfWorkService, QuestionService questionService, QuizFactory quizFactory) {
        /* must add more verifications */
        Quiz oldQuiz = (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(quizAggregateId, unitOfWork);
        QuizDto quizDto = quizFactory.createQuizDto(oldQuiz);
        List<QuestionDto> questionDtoList = new ArrayList<>();
        // TODO if I have time change the quiz to only store references to the questions
        // (its easier)
        oldQuiz.getQuizQuestions().forEach(quizQuestion -> {
            QuestionDto questionDto = questionService.getQuestionById(quizQuestion.getQuestionAggregateId(),
                    unitOfWork);
            questionDto.getOptionDtos().forEach(o -> {
                o.setCorrect(false); // by setting all to false frontend doesn't know which is correct
            });
            questionDtoList.add(questionDto);
        });
        quizDto.setQuestionDtos(questionDtoList);
        return quizDto;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public QuizDto createQuizTransactional(QuizCourseExecution quizCourseExecution, Set<QuestionDto> questions,
            QuizDto quizDto, UnitOfWork unitOfWork,
            AggregateIdGeneratorService aggregateIdGeneratorService, QuizFactory quizFactory,
            UnitOfWorkService<UnitOfWork> unitOfWorkService) {
        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();

        Set<QuizQuestion> quizQuestions = questions.stream()
                .map(QuizQuestion::new)
                .collect(Collectors.toSet());

        Quiz quiz = quizFactory.createQuiz(aggregateId, quizCourseExecution, quizQuestions, quizDto, IN_CLASS);
        unitOfWorkService.registerChanged(quiz, unitOfWork);
        return quizFactory.createQuizDto(quiz);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public QuizDto updateGeneratedQuizTransactional(QuizDto quizDto, Set<Integer> topicsAggregateIds,
            Integer numberOfQuestions, UnitOfWork unitOfWork,
            UnitOfWorkService<UnitOfWork> unitOfWorkService, QuestionService questionService, QuizFactory quizFactory) {
        Quiz oldQuiz = (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(quizDto.getAggregateId(), unitOfWork);
        Quiz newQuiz = quizFactory.createQuizFromExisting(oldQuiz);
        newQuiz.update(quizDto);

        if (topicsAggregateIds != null && numberOfQuestions != null) {
            List<QuestionDto> questionDtos = questionService
                    .findQuestionsByTopicIds(new ArrayList<>(topicsAggregateIds), unitOfWork);

            if (questionDtos.size() < numberOfQuestions) {
                throw new QuizzesException(QuizzesErrorMessage.NOT_ENOUGH_QUESTIONS);
            }

            Set<QuizQuestion> quizQuestions = questionDtos.stream()
                    .map(QuizQuestion::new)
                    .collect(Collectors.toSet());

            newQuiz.setQuizQuestions(quizQuestions);
        }

        newQuiz.setTitle("Generated Quiz Title");
        unitOfWorkService.registerChanged(newQuiz, unitOfWork);
        return quizFactory.createQuizDto(newQuiz);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public QuizDto updateQuizTransactional(QuizDto quizDto, Set<QuizQuestion> quizQuestions, UnitOfWork unitOfWork,
            UnitOfWorkService<UnitOfWork> unitOfWorkService, QuizFactory quizFactory) {
        Quiz oldQuiz = (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(quizDto.getAggregateId(), unitOfWork);
        Quiz newQuiz = quizFactory.createQuizFromExisting(oldQuiz);

        if (quizDto.getTitle() != null) {
            newQuiz.setTitle(quizDto.getTitle());
            unitOfWorkService.registerChanged(newQuiz, unitOfWork);
        }

        if (quizDto.getAvailableDate() != null) {
            newQuiz.setAvailableDate(LocalDateTime.parse(quizDto.getAvailableDate()));
        }

        if (quizDto.getConclusionDate() != null) {
            newQuiz.setConclusionDate(LocalDateTime.parse(quizDto.getConclusionDate()));
        }

        if (quizDto.getResultsDate() != null) {
            newQuiz.setResultsDate(LocalDateTime.parse(quizDto.getResultsDate()));
        }

        if (quizQuestions != null && quizQuestions.size() > 0) {
            newQuiz.setQuizQuestions(quizQuestions);
        }

        return quizFactory.createQuizDto(newQuiz);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<QuizDto> getAvailableQuizzesTransactional(Integer courseExecutionAggregateId, UnitOfWork unitOfWork,
            QuizRepository quizRepository, UnitOfWorkService<UnitOfWork> unitOfWorkService) {
        LocalDateTime now = DateHandler.now();
        return quizRepository.findAllQuizIdsByCourseExecution(courseExecutionAggregateId).stream()
                .map(id -> (Quiz) unitOfWorkService.aggregateLoad(id, unitOfWork))
                .filter(quiz -> quiz.getAvailableDate().isAfter(now) && quiz.getConclusionDate().isBefore(now)
                        && quiz.getQuizType() != GENERATED)
                .map(quiz -> (Quiz) unitOfWorkService.registerRead(quiz, unitOfWork))
                .map(QuizDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public QuizDto removeCourseExecutionTransactional(Integer quizAggregateId, Integer courseExecutionId,
            Integer aggregateVersion, UnitOfWork unitOfWork,
            UnitOfWorkService<UnitOfWork> unitOfWorkService, QuizFactory quizFactory) {
        Quiz oldQuiz = (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(quizAggregateId, unitOfWork);
        Quiz newQuiz = quizFactory.createQuizFromExisting(oldQuiz);

        if (newQuiz.getQuizCourseExecution().getCourseExecutionAggregateId().equals(courseExecutionId)) {
            newQuiz.setState(Aggregate.AggregateState.INACTIVE);
            unitOfWorkService.registerChanged(newQuiz, unitOfWork);
            return new QuizDto(newQuiz);
        }

        return null;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void updateQuestionTransactional(Integer quizAggregateId, Integer questionAggregateId, String title,
            String content, Integer aggregateVersion, UnitOfWork unitOfWork,
            UnitOfWorkService<UnitOfWork> unitOfWorkService, QuizFactory quizFactory) {
        Quiz oldQuiz = (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(quizAggregateId, unitOfWork);
        Quiz newQuiz = quizFactory.createQuizFromExisting(oldQuiz);

        QuizQuestion quizQuestion = newQuiz.findQuestion(questionAggregateId);

        if (quizQuestion != null) {
            quizQuestion.setTitle(title);
            quizQuestion.setContent(content);
            quizQuestion.setQuestionVersion(aggregateVersion);
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void removeQuizQuestionTransactional(Integer quizAggregateId, Integer questionAggregateId,
            UnitOfWork unitOfWork,
            UnitOfWorkService<UnitOfWork> unitOfWorkService, QuizFactory quizFactory) {
        Quiz oldQuiz = (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(quizAggregateId, unitOfWork);
        Quiz newQuiz = quizFactory.createQuizFromExisting(oldQuiz);

        QuizQuestion quizQuestion = newQuiz.findQuestion(questionAggregateId);

        if (quizQuestion != null) {
            newQuiz.setState(Aggregate.AggregateState.INACTIVE);
            quizQuestion.setState(Aggregate.AggregateState.DELETED);
            unitOfWorkService.registerEvent(new InvalidateQuizEvent(newQuiz.getAggregateId()), unitOfWork);
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void removeQuizTransactional(Integer quizAggregateId, UnitOfWork unitOfWork,
            UnitOfWorkService<UnitOfWork> unitOfWorkService, QuizFactory quizFactory) {
        Quiz oldQuiz = (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(quizAggregateId, unitOfWork);
        Quiz newQuiz = quizFactory.createQuizFromExisting(oldQuiz);
        newQuiz.remove();
        unitOfWorkService.registerChanged(newQuiz, unitOfWork);
    }
}

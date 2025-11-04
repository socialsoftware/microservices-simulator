package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.service;

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
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.events.publish.QuizAnswerQuestionAnswerEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesException;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;

import java.sql.SQLException;

@Service
public class QuizAnswerService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;
    @Autowired
    private QuizService quizService;
    @Autowired
    private CourseExecutionService courseExecutionService;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    private final QuizAnswerCustomRepository quizAnswerRepository;

    private final QuizAnswerTransactionalService quizAnswerTransactionalService = new QuizAnswerTransactionalService();

    @Autowired
    private QuizAnswerFactory quizAnswerFactory;

    public QuizAnswerService(UnitOfWorkService unitOfWorkService, QuizAnswerCustomRepository quizAnswerRepository) {
        this.unitOfWorkService = unitOfWorkService;
        this.quizAnswerRepository = quizAnswerRepository;
    }

    public QuizAnswerDto getQuizAnswerDtoByQuizIdAndUserId(Integer quizAggregateId, Integer userAggregateId,
            UnitOfWork unitOfWork) {
        Integer quizAnswerId = quizAnswerRepository.findQuizAnswerIdByQuizIdAndUserId(quizAggregateId, userAggregateId)
                .orElseThrow(() -> new QuizzesException(QuizzesErrorMessage.NO_USER_ANSWER_FOR_QUIZ, quizAggregateId,
                        userAggregateId));

        QuizAnswer quizAnswer = (QuizAnswer) unitOfWorkService.aggregateLoadAndRegisterRead(quizAnswerId, unitOfWork);

        if (quizAnswer.getState() == Aggregate.AggregateState.DELETED) {
            throw new QuizzesException(QuizzesErrorMessage.QUIZ_ANSWER_DELETED, quizAnswer.getAggregateId());
        }

        return quizAnswerFactory.createQuizAnswerDto(quizAnswer);
    }

    @Retryable(retryFor = {
            TransientDataAccessException.class,
            SQLException.class }, maxAttempts = 5, backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000))
    public QuizAnswerDto startQuiz(Integer quizAggregateId, Integer courseExecutionAggregateId, Integer userAggregateId,
            UnitOfWork unitOfWork) {
        return quizAnswerTransactionalService.startQuizTransactional(quizAggregateId, courseExecutionAggregateId,
                userAggregateId, unitOfWork, aggregateIdGeneratorService, quizService, courseExecutionService,
                quizAnswerFactory, unitOfWorkService);
    }

    @Retryable(retryFor = {
            TransientDataAccessException.class,
            SQLException.class }, maxAttempts = 5, backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000))
    public void answerQuestion(Integer quizAggregateId, Integer userAggregateId, QuestionAnswerDto userAnswerDto,
            QuestionDto questionDto, UnitOfWork unitOfWork) {
        quizAnswerTransactionalService.answerQuestionTransactional(quizAggregateId, userAggregateId, userAnswerDto,
                questionDto, unitOfWork, unitOfWorkService, quizAnswerFactory, quizAnswerRepository);
    }

    @Retryable(retryFor = {
            TransientDataAccessException.class,
            SQLException.class }, maxAttempts = 5, backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000))
    public void concludeQuiz(Integer quizAggregateId, Integer userAggregateId, UnitOfWork unitOfWork) {
        quizAnswerTransactionalService.concludeQuizTransactional(quizAggregateId, userAggregateId, unitOfWork,
                unitOfWorkService, quizAnswerFactory, quizAnswerRepository);
    }

    @Retryable(retryFor = {
            TransientDataAccessException.class,
            SQLException.class }, maxAttempts = 5, backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000))
    public void removeQuizAnswer(Integer quizAnswerAggregateId, UnitOfWork unitOfWork) {
        quizAnswerTransactionalService.removeQuizAnswerTransactional(quizAnswerAggregateId, unitOfWork,
                unitOfWorkService, quizAnswerFactory);
    }

    /************************************************
     * EVENT PROCESSING
     ************************************************/

    @Retryable(retryFor = {
            TransientDataAccessException.class,
            SQLException.class }, maxAttempts = 5, backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000))
    public void updateUserName(Integer answerAggregateId, Integer executionAggregateId, Integer eventVersion,
            Integer userAggregateId, String name, UnitOfWork unitOfWork) {
        quizAnswerTransactionalService.updateUserNameTransactional(answerAggregateId, executionAggregateId,
                eventVersion, userAggregateId, name, unitOfWork, unitOfWorkService, quizAnswerFactory);
    }

    @Retryable(retryFor = {
            TransientDataAccessException.class,
            SQLException.class }, maxAttempts = 5, backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000))
    public QuizAnswer removeUser(Integer answerAggregateId, Integer userAggregateId, Integer aggregateVersion,
            UnitOfWork unitOfWork) {
        return quizAnswerTransactionalService.removeUserTransactional(answerAggregateId, userAggregateId,
                aggregateVersion, unitOfWork, unitOfWorkService, quizAnswerFactory);
    }

    @Retryable(retryFor = {
            TransientDataAccessException.class,
            SQLException.class }, maxAttempts = 5, backoff = @Backoff(delay = 200, multiplier = 2, maxDelay = 2000))
    public QuizAnswer removeQuestion(Integer answerAggregateId, Integer questionAggregateId, Integer aggregateVersion,
            UnitOfWork unitOfWork) {
        return quizAnswerTransactionalService.removeQuestionTransactional(answerAggregateId, questionAggregateId,
                aggregateVersion, unitOfWork, unitOfWorkService, quizAnswerFactory);
    }

}

@Service
class QuizAnswerTransactionalService {

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public QuizAnswerDto startQuizTransactional(Integer quizAggregateId, Integer courseExecutionAggregateId,
            Integer userAggregateId, UnitOfWork unitOfWork,
            AggregateIdGeneratorService aggregateIdGeneratorService, QuizService quizService,
            CourseExecutionService courseExecutionService,
            QuizAnswerFactory quizAnswerFactory, UnitOfWorkService<UnitOfWork> unitOfWorkService) {
        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        QuizDto quizDto = quizService.getQuizById(quizAggregateId, unitOfWork);

        // COURSE_EXECUTION_SAME_QUIZ_COURSE_EXECUTION
        if (!courseExecutionAggregateId.equals(quizDto.getCourseExecutionAggregateId())) {
            throw new QuizzesException(QuizzesErrorMessage.QUIZ_DOES_NOT_BELONG_TO_COURSE_EXECUTION, quizAggregateId,
                    courseExecutionAggregateId);
        }

        // QUIZ_COURSE_EXECUTION_SAME_AS_USER_COURSE_EXECUTION
        // COURSE_EXECUTION_SAME_AS_USER_COURSE_EXECUTION
        UserDto userDto = courseExecutionService
                .getStudentByExecutionIdAndUserId(quizDto.getCourseExecutionAggregateId(), userAggregateId, unitOfWork);

        // QUESTIONS_ANSWER_QUESTIONS_BELONG_TO_QUIZ because questions come from the
        // quiz
        QuizAnswer quizAnswer = quizAnswerFactory.createQuizAnswer(aggregateId,
                new AnswerCourseExecution(quizDto.getCourseExecutionAggregateId(), quizDto.getCourseExecutionVersion()),
                new AnswerStudent(userDto), new AnsweredQuiz(quizDto));

        quizAnswer.setAnswerDate(DateHandler.now());

        unitOfWorkService.registerChanged(quizAnswer, unitOfWork);
        return quizAnswerFactory.createQuizAnswerDto(quizAnswer);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void answerQuestionTransactional(Integer quizAggregateId, Integer userAggregateId,
            QuestionAnswerDto userAnswerDto, QuestionDto questionDto, UnitOfWork unitOfWork,
            UnitOfWorkService<UnitOfWork> unitOfWorkService, QuizAnswerFactory quizAnswerFactory,
            QuizAnswerCustomRepository quizAnswerRepository) {
        QuizAnswer oldQuizAnswer = getQuizAnswerByQuizIdAndUserIdTransactional(quizAggregateId, userAggregateId,
                unitOfWork, unitOfWorkService, quizAnswerRepository);
        QuizAnswer newQuizAnswer = quizAnswerFactory.createQuizAnswerFromExisting(oldQuizAnswer);

        QuestionAnswer questionAnswer = new QuestionAnswer(userAnswerDto, questionDto);
        newQuizAnswer.addQuestionAnswer(questionAnswer);
        unitOfWorkService.registerChanged(newQuizAnswer, unitOfWork);
        unitOfWorkService.registerEvent(new QuizAnswerQuestionAnswerEvent(newQuizAnswer.getAggregateId(),
                questionAnswer.getQuestionAggregateId(), quizAggregateId, userAggregateId, questionAnswer.isCorrect()),
                unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void concludeQuizTransactional(Integer quizAggregateId, Integer userAggregateId, UnitOfWork unitOfWork,
            UnitOfWorkService<UnitOfWork> unitOfWorkService, QuizAnswerFactory quizAnswerFactory,
            QuizAnswerCustomRepository quizAnswerRepository) {
        QuizAnswer oldQuizAnswer = getQuizAnswerByQuizIdAndUserIdTransactional(quizAggregateId, userAggregateId,
                unitOfWork, unitOfWorkService, quizAnswerRepository);
        QuizAnswer newQuizAnswer = quizAnswerFactory.createQuizAnswerFromExisting(oldQuizAnswer);

        newQuizAnswer.setCompleted(true);
        unitOfWorkService.registerChanged(newQuizAnswer, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void removeQuizAnswerTransactional(Integer quizAnswerAggregateId, UnitOfWork unitOfWork,
            UnitOfWorkService<UnitOfWork> unitOfWorkService, QuizAnswerFactory quizAnswerFactory) {
        QuizAnswer oldQuizAnswer = (QuizAnswer) unitOfWorkService.aggregateLoadAndRegisterRead(quizAnswerAggregateId,
                unitOfWork);
        QuizAnswer newQuizAnswer = quizAnswerFactory.createQuizAnswerFromExisting(oldQuizAnswer);
        newQuizAnswer.remove();
        unitOfWorkService.registerChanged(newQuizAnswer, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void updateUserNameTransactional(Integer answerAggregateId, Integer executionAggregateId,
            Integer eventVersion, Integer userAggregateId, String name, UnitOfWork unitOfWork,
            UnitOfWorkService<UnitOfWork> unitOfWorkService, QuizAnswerFactory quizAnswerFactory) {
        QuizAnswer oldQuizAnswer = (QuizAnswer) unitOfWorkService.aggregateLoadAndRegisterRead(answerAggregateId,
                unitOfWork);
        QuizAnswer newQuizAnswer = quizAnswerFactory.createQuizAnswerFromExisting(oldQuizAnswer);

        if (!newQuizAnswer.getAnswerCourseExecution().getCourseExecutionAggregateId().equals(executionAggregateId)) {
            return;
        }

        if (newQuizAnswer.getStudent().getStudentAggregateId().equals(userAggregateId)) {
            newQuizAnswer.getStudent().setName(name);
            newQuizAnswer.getAnswerCourseExecution().setCourseExecutionVersion(eventVersion);
            unitOfWorkService.registerChanged(newQuizAnswer, unitOfWork);
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public QuizAnswer removeUserTransactional(Integer answerAggregateId, Integer userAggregateId,
            Integer aggregateVersion, UnitOfWork unitOfWork,
            UnitOfWorkService<UnitOfWork> unitOfWorkService, QuizAnswerFactory quizAnswerFactory) {
        QuizAnswer oldQuizAnswer = (QuizAnswer) unitOfWorkService.aggregateLoadAndRegisterRead(answerAggregateId,
                unitOfWork);
        if (oldQuizAnswer != null && oldQuizAnswer.getStudent().getStudentAggregateId().equals(userAggregateId)
                && oldQuizAnswer.getVersion() >= aggregateVersion) {
            return null;
        }

        QuizAnswer newQuizAnswer = quizAnswerFactory.createQuizAnswerFromExisting(oldQuizAnswer);
        newQuizAnswer.getStudent().setStudentState(Aggregate.AggregateState.DELETED);
        newQuizAnswer.setState(Aggregate.AggregateState.INACTIVE);
        unitOfWorkService.registerChanged(newQuizAnswer, unitOfWork);
        return newQuizAnswer;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public QuizAnswer removeQuestionTransactional(Integer answerAggregateId, Integer questionAggregateId,
            Integer aggregateVersion, UnitOfWork unitOfWork,
            UnitOfWorkService<UnitOfWork> unitOfWorkService, QuizAnswerFactory quizAnswerFactory) {
        QuizAnswer oldQuizAnswer = (QuizAnswer) unitOfWorkService.aggregateLoadAndRegisterRead(answerAggregateId,
                unitOfWork);
        QuestionAnswer questionAnswer = oldQuizAnswer.findQuestionAnswer(questionAggregateId);

        if (questionAnswer == null) {
            return null;
        }

        QuizAnswer newQuizAnswer = quizAnswerFactory.createQuizAnswerFromExisting(oldQuizAnswer);
        questionAnswer.setState(Aggregate.AggregateState.DELETED);
        newQuizAnswer.setState(Aggregate.AggregateState.INACTIVE);
        unitOfWorkService.registerChanged(newQuizAnswer, unitOfWork);
        return newQuizAnswer;
    }

    private QuizAnswer getQuizAnswerByQuizIdAndUserIdTransactional(Integer quizAggregateId, Integer userAggregateId,
            UnitOfWork unitOfWork,
            UnitOfWorkService<UnitOfWork> unitOfWorkService, QuizAnswerCustomRepository quizAnswerRepository) {
        Integer quizAnswerId = quizAnswerRepository.findQuizAnswerIdByQuizIdAndUserId(quizAggregateId, userAggregateId)
                .orElseThrow(() -> new QuizzesException(QuizzesErrorMessage.NO_USER_ANSWER_FOR_QUIZ, quizAggregateId,
                        userAggregateId));

        QuizAnswer quizAnswer = (QuizAnswer) unitOfWorkService.aggregateLoadAndRegisterRead(quizAnswerId, unitOfWork);

        if (quizAnswer.getState() == Aggregate.AggregateState.DELETED) {
            throw new QuizzesException(QuizzesErrorMessage.QUIZ_ANSWER_DELETED, quizAnswer.getAggregateId());
        }

        return quizAnswer;
    }
}

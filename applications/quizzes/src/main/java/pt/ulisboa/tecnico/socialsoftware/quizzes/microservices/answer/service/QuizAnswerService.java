package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.CannotAcquireLockException;
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

    @Autowired
    private QuizAnswerFactory quizAnswerFactory;

    public QuizAnswerService(UnitOfWorkService unitOfWorkService, QuizAnswerCustomRepository quizAnswerRepository) {
        this.unitOfWorkService = unitOfWorkService;
        this.quizAnswerRepository = quizAnswerRepository;
    }

    private QuizAnswer getQuizAnswerByQuizIdAndUserId(Integer quizAggregateId, Integer userAggregateId, UnitOfWork unitOfWork) {
        Integer quizAnswerId = quizAnswerRepository.findQuizAnswerIdByQuizIdAndUserId(quizAggregateId, userAggregateId)
                .orElseThrow(() -> new QuizzesException(QuizzesErrorMessage.NO_USER_ANSWER_FOR_QUIZ, quizAggregateId, userAggregateId));

        QuizAnswer quizAnswer = (QuizAnswer) unitOfWorkService.aggregateLoadAndRegisterRead(quizAnswerId, unitOfWork);

        if (quizAnswer.getState() == Aggregate.AggregateState.DELETED) {
            throw new QuizzesException(QuizzesErrorMessage.QUIZ_ANSWER_DELETED, quizAnswer.getAggregateId());
        }

        return quizAnswer;
    }

    public QuizAnswerDto getQuizAnswerDtoByQuizIdAndUserId(Integer quizAggregateId, Integer userAggregateId, UnitOfWork unitOfWork) {
        Integer quizAnswerId = quizAnswerRepository.findQuizAnswerIdByQuizIdAndUserId(quizAggregateId, userAggregateId)
                .orElseThrow(() -> new QuizzesException(QuizzesErrorMessage.NO_USER_ANSWER_FOR_QUIZ, quizAggregateId, userAggregateId));

        QuizAnswer quizAnswer = (QuizAnswer) unitOfWorkService.aggregateLoadAndRegisterRead(quizAnswerId, unitOfWork);

        if (quizAnswer.getState() == Aggregate.AggregateState.DELETED) {
            throw new QuizzesException(QuizzesErrorMessage.QUIZ_ANSWER_DELETED, quizAnswer.getAggregateId());
        }

        return quizAnswerFactory.createQuizAnswerDto(quizAnswer);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public QuizAnswerDto startQuiz(Integer quizAggregateId, Integer courseExecutionAggregateId, Integer userAggregateId, UnitOfWork unitOfWork) {
        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        QuizDto quizDto = quizService.getQuizById(quizAggregateId, unitOfWork);

        // COURSE_EXECUTION_SAME_QUIZ_COURSE_EXECUTION
        if (!courseExecutionAggregateId.equals(quizDto.getCourseExecutionAggregateId())) {
            throw new QuizzesException(QuizzesErrorMessage.QUIZ_DOES_NOT_BELONG_TO_COURSE_EXECUTION, quizAggregateId, courseExecutionAggregateId);
        }

        // QUIZ_COURSE_EXECUTION_SAME_AS_USER_COURSE_EXECUTION
        // COURSE_EXECUTION_SAME_AS_USER_COURSE_EXECUTION
        UserDto userDto = courseExecutionService.getStudentByExecutionIdAndUserId(quizDto.getCourseExecutionAggregateId(), userAggregateId, unitOfWork);

        // QUESTIONS_ANSWER_QUESTIONS_BELONG_TO_QUIZ because questions come from the quiz
        QuizAnswer quizAnswer = quizAnswerFactory.createQuizAnswer(aggregateId, new AnswerCourseExecution(quizDto.getCourseExecutionAggregateId(), quizDto.getCourseExecutionVersion()), new AnswerStudent(userDto), new AnsweredQuiz(quizDto));
        
        quizAnswer.setAnswerDate(DateHandler.now());

        unitOfWorkService.registerChanged(quizAnswer, unitOfWork);
        return quizAnswerFactory.createQuizAnswerDto(quizAnswer);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void answerQuestion(Integer quizAggregateId, Integer userAggregateId, QuestionAnswerDto userAnswerDto, QuestionDto questionDto, UnitOfWork unitOfWork) {
        QuizAnswer oldQuizAnswer = getQuizAnswerByQuizIdAndUserId(quizAggregateId, userAggregateId, unitOfWork);
        QuizAnswer newQuizAnswer = quizAnswerFactory.createQuizAnswerFromExisting(oldQuizAnswer);

        QuestionAnswer questionAnswer = new QuestionAnswer(userAnswerDto, questionDto);
        newQuizAnswer.addQuestionAnswer(questionAnswer);
        unitOfWorkService.registerChanged(newQuizAnswer, unitOfWork);
        unitOfWorkService.registerEvent(new QuizAnswerQuestionAnswerEvent(newQuizAnswer.getAggregateId(), questionAnswer.getQuestionAggregateId(), quizAggregateId, userAggregateId, questionAnswer.isCorrect()), unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void concludeQuiz(Integer quizAggregateId, Integer userAggregateId, UnitOfWork unitOfWork) {
        QuizAnswer oldQuizAnswer = getQuizAnswerByQuizIdAndUserId(quizAggregateId, userAggregateId, unitOfWork);
        QuizAnswer newQuizAnswer = quizAnswerFactory.createQuizAnswerFromExisting(oldQuizAnswer);

        newQuizAnswer.setCompleted(true);
        unitOfWorkService.registerChanged(newQuizAnswer, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void removeQuizAnswer(Integer quizAnswerAggregateId, UnitOfWork unitOfWork) {
        QuizAnswer oldQuizAnswer = (QuizAnswer) unitOfWorkService.aggregateLoadAndRegisterRead(quizAnswerAggregateId, unitOfWork);
        QuizAnswer newQuizAnsewer = quizAnswerFactory.createQuizAnswerFromExisting(oldQuizAnswer);
        newQuizAnsewer.remove();
        unitOfWorkService.registerChanged(newQuizAnsewer, unitOfWork);
    }

    /************************************************ EVENT PROCESSING ************************************************/

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void updateUserName(Integer answerAggregateId, Integer executionAggregateId, Integer eventVersion, Integer userAggregateId, String name, UnitOfWork unitOfWork) {
        QuizAnswer oldQuizAnswer = (QuizAnswer) unitOfWorkService.aggregateLoadAndRegisterRead(answerAggregateId, unitOfWork);
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
    public QuizAnswer removeUser(Integer answerAggregateId, Integer userAggregateId, Integer aggregateVersion, UnitOfWork unitOfWork) {
        QuizAnswer oldQuizAnswer = (QuizAnswer) unitOfWorkService.aggregateLoadAndRegisterRead(answerAggregateId, unitOfWork);
        if (oldQuizAnswer != null && oldQuizAnswer.getStudent().getStudentAggregateId().equals(userAggregateId) && oldQuizAnswer.getVersion() >= aggregateVersion) {
            return null;
        }

        QuizAnswer newQuizAnswer = quizAnswerFactory.createQuizAnswerFromExisting(oldQuizAnswer);
        newQuizAnswer.getStudent().setStudentState(Aggregate.AggregateState.DELETED);
        newQuizAnswer.setState(Aggregate.AggregateState.INACTIVE);
        unitOfWorkService.registerChanged(newQuizAnswer, unitOfWork);
        return newQuizAnswer;
    }

    public QuizAnswer removeQuestion(Integer answerAggregateId, Integer questionAggregateId, Integer aggregateVersion, UnitOfWork unitOfWork) {
        QuizAnswer oldQuizAnswer = (QuizAnswer) unitOfWorkService.aggregateLoadAndRegisterRead(answerAggregateId, unitOfWork);
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
}

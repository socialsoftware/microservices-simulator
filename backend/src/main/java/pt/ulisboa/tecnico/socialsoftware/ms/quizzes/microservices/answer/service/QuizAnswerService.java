package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.events.publish.QuizAnswerQuestionAnswerEvent;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.course.aggregate.CourseCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.aggregates.CausalQuizAnswer;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;

import java.sql.SQLException;
import java.time.LocalDateTime;

@Service
public class QuizAnswerService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;
    @Autowired
    private QuizService quizService;
    @Autowired
    private CourseExecutionService courseExecutionService;

    private final UnitOfWorkService unitOfWorkService;

    private final QuizAnswerCustomRepository quizAnswerRepository;

    @Autowired
    private QuizAnswerFactory quizAnswerFactory;

    public QuizAnswerService(UnitOfWorkService unitOfWorkService, QuizAnswerCustomRepository quizAnswerRepository) {
        this.unitOfWorkService = unitOfWorkService;
        this.quizAnswerRepository = quizAnswerRepository;
    }

    public QuizAnswer getQuizAnswerByQuizIdAndUserId(Integer quizAggregateId, Integer userAggregateId, UnitOfWork unitOfWork) {
        Integer quizAnswerId = quizAnswerRepository.findQuizAnswerIdByQuizIdAndUserId(quizAggregateId, userAggregateId)
                .orElseThrow(() -> new TutorException(ErrorMessage.NO_USER_ANSWER_FOR_QUIZ, quizAggregateId, userAggregateId));

        QuizAnswer quizAnswer = (QuizAnswer) unitOfWorkService.aggregateLoadAndRegisterRead(quizAnswerId, unitOfWork);

        if (quizAnswer.getState() == Aggregate.AggregateState.DELETED) {
            throw new TutorException(ErrorMessage.QUIZ_ANSWER_DELETED, quizAnswer.getAggregateId());
        }

        return quizAnswer;
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public QuizAnswerDto startQuiz(Integer quizAggregateId, Integer courseExecutionAggregateId, Integer userAggregateId, UnitOfWork unitOfWork) {
        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        QuizDto quizDto = quizService.getQuizById(quizAggregateId, unitOfWork);

        // COURSE_EXECUTION_SAME_QUIZ_COURSE_EXECUTION
        if (!courseExecutionAggregateId.equals(quizDto.getAggregateId())) {
            throw new TutorException(ErrorMessage.QUIZ_DOES_NOT_BELONG_TO_COURSE_EXECUTION, quizAggregateId, courseExecutionAggregateId);
        }

        // QUIZ_COURSE_EXECUTION_SAME_AS_USER_COURSE_EXECUTION
        // COURSE_EXECUTION_SAME_AS_USER_COURSE_EXECUTION
        UserDto userDto = courseExecutionService.getStudentByExecutionIdAndUserId(userAggregateId, quizDto.getCourseExecutionAggregateId(), unitOfWork);

        // QUESTIONS_ANSWER_QUESTIONS_BELONG_TO_QUIZ because questions come from the quiz
        QuizAnswer quizAnswer = quizAnswerFactory.createQuizAnswer(aggregateId, new AnswerCourseExecution(quizDto.getCourseExecutionAggregateId(), quizDto.getCourseExecutionVersion()), new AnswerStudent(userDto), new AnsweredQuiz(quizDto));
        quizAnswer.setAnswerDate(LocalDateTime.now());
        unitOfWork.registerChanged(quizAnswer);
        return new QuizAnswerDto(quizAnswer);
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void answerQuestion(Integer quizAggregateId, Integer userAggregateId, QuestionAnswerDto userAnswerDto, QuestionDto questionDto, UnitOfWork unitOfWork) {
        QuizAnswer oldQuizAnswer = getQuizAnswerByQuizIdAndUserId(quizAggregateId, userAggregateId, unitOfWork);
        QuizAnswer newQuizAnswer = quizAnswerFactory.createQuizAnswerFromExisting(oldQuizAnswer);

        QuestionAnswer questionAnswer = new QuestionAnswer(userAnswerDto, questionDto);
        newQuizAnswer.addQuestionAnswer(questionAnswer);
        unitOfWork.registerChanged(newQuizAnswer);
        unitOfWork.addEvent(new QuizAnswerQuestionAnswerEvent(newQuizAnswer.getAggregateId(), questionAnswer.getQuestionAggregateId(), quizAggregateId, userAggregateId, questionAnswer.isCorrect()));
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void concludeQuiz(Integer quizAggregateId, Integer userAggregateId, UnitOfWork unitOfWork) {
        QuizAnswer oldQuizAnswer = getQuizAnswerByQuizIdAndUserId(quizAggregateId, userAggregateId, unitOfWork);
        QuizAnswer newQuizAnswer = quizAnswerFactory.createQuizAnswerFromExisting(oldQuizAnswer);

        newQuizAnswer.setCompleted(true);
        unitOfWork.registerChanged(newQuizAnswer);
    }

    /************************************************ EVENT PROCESSING ************************************************/

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void updateUserName(Integer answerAggregateId, Integer executionAggregateId, Integer eventVersion, Integer userAggregateId, String name, UnitOfWork unitOfWork) {
        QuizAnswer oldQuizAnswer = (QuizAnswer) unitOfWorkService.aggregateLoadAndRegisterRead(answerAggregateId, unitOfWork);
        QuizAnswer newQuizAnswer = quizAnswerFactory.createQuizAnswerFromExisting(oldQuizAnswer);

        if (!newQuizAnswer.getAnswerCourseExecution().getCourseExecutionAggregateId().equals(executionAggregateId)) {
            return;
        }

        if (newQuizAnswer.getStudent().getStudentAggregateId().equals(userAggregateId)) {
            newQuizAnswer.getStudent().setName(name);
            newQuizAnswer.getAnswerCourseExecution().setCourseExecutionVersion(eventVersion);
            unitOfWork.registerChanged(newQuizAnswer);
        }
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public QuizAnswer removeUser(Integer answerAggregateId, Integer userAggregateId, Integer aggregateVersion, UnitOfWork unitOfWork) {
        QuizAnswer oldQuizAnswer = (QuizAnswer) unitOfWorkService.aggregateLoadAndRegisterRead(answerAggregateId, unitOfWork);
        if (oldQuizAnswer != null && oldQuizAnswer.getStudent().getStudentAggregateId().equals(userAggregateId) && oldQuizAnswer.getVersion() >= aggregateVersion) {
            return null;
        }

        QuizAnswer newQuizAnswer = quizAnswerFactory.createQuizAnswerFromExisting(oldQuizAnswer);
        newQuizAnswer.getStudent().setStudentState(Aggregate.AggregateState.DELETED);
        newQuizAnswer.setState(Aggregate.AggregateState.INACTIVE);
        unitOfWork.registerChanged(newQuizAnswer);
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
        unitOfWork.registerChanged(newQuizAnswer);
        return newQuizAnswer;
    }
}

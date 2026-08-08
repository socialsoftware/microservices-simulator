package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.QuizAnswerQuestionAnswerEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2ErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2Exception;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate.QuestionAnswer;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate.QuestionAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate.QuizAnswer;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate.QuizAnswerCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate.QuizAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate.QuizAnswerFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.UserDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class QuizAnswerService {
    private final QuizAnswerCustomRepository quizAnswerCustomRepository;
    private final QuizAnswerFactory quizAnswerFactory;
    private final UnitOfWorkService unitOfWorkService;
    private final AggregateIdGeneratorService aggregateIdGeneratorService;

    public QuizAnswerService(QuizAnswerCustomRepository quizAnswerCustomRepository,
                             QuizAnswerFactory quizAnswerFactory,
                             UnitOfWorkService unitOfWorkService,
                             AggregateIdGeneratorService aggregateIdGeneratorService) {
        this.quizAnswerCustomRepository = quizAnswerCustomRepository;
        this.quizAnswerFactory = quizAnswerFactory;
        this.unitOfWorkService = unitOfWorkService;
        this.aggregateIdGeneratorService = aggregateIdGeneratorService;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public QuizAnswerDto getQuizAnswerById(Integer quizAnswerAggregateId, UnitOfWork unitOfWork) {
        return quizAnswerFactory.createQuizAnswerDto(
                (QuizAnswer) unitOfWorkService.aggregateLoadAndRegisterRead(quizAnswerAggregateId, unitOfWork));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public QuizAnswerDto getQuizAnswerForStudentAndQuiz(Integer userAggregateId, Integer quizAggregateId,
                                                       UnitOfWork unitOfWork) {
        Integer quizAnswerAggregateId = quizAnswerCustomRepository
                .findQuizAnswerIdByStudentAndQuiz(userAggregateId, quizAggregateId)
                .orElseThrow(() -> new QuizzesFull2Exception(QuizzesFull2ErrorMessage.QUIZ_ANSWER_NOT_FOUND));

        return getQuizAnswerById(quizAnswerAggregateId, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public QuizAnswerDto createQuizAnswer(QuizDto quizDto, UserDto userDto, ExecutionDto executionDto,
                                          List<QuestionAnswerDto> questionAnswers, UnitOfWork unitOfWork) {
        if (!Objects.equals(quizDto.getExecutionAggregateId(), executionDto.getAggregateId())) {
            throw new QuizzesFull2Exception(
                    QuizzesFull2ErrorMessage.COURSE_EXECUTION_SAME_QUIZ_COURSE_EXECUTION);
        }
        if (quizAnswerCustomRepository
                .findQuizAnswerIdByStudentAndQuiz(userDto.getAggregateId(), quizDto.getAggregateId())
                .isPresent()) {
            throw new QuizzesFull2Exception(QuizzesFull2ErrorMessage.UNIQUE_QUIZ_ANSWER_PER_STUDENT);
        }

        LocalDateTime now = DateHandler.now();
        QuizAnswer quizAnswer = quizAnswerFactory.createQuizAnswer(
                aggregateIdGeneratorService.getNewAggregateId(),
                quizDto.getAggregateId(), quizDto.getVersion(),
                userDto.getAggregateId(), userDto.getName(), userDto.getVersion(),
                executionDto.getAggregateId(), executionDto.getVersion(), now, now);

        questionAnswers.forEach(questionAnswerDto -> quizAnswer.addQuestionAnswer(
                new QuestionAnswer(questionAnswerDto.getQuestionAggregateId(),
                        questionAnswerDto.getQuestionVersion(), questionAnswerDto.getCorrectOptionKey())));

        unitOfWorkService.registerChanged(quizAnswer, unitOfWork);
        return quizAnswerFactory.createQuizAnswerDto(quizAnswer);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void answerQuestion(Integer quizAnswerAggregateId, Integer questionAggregateId,
                               Integer optionSequenceChoice, Integer optionKey, Integer timeTaken,
                               UnitOfWork unitOfWork) {
        QuizAnswer oldQuizAnswer = (QuizAnswer) unitOfWorkService.aggregateLoadAndRegisterRead(
                quizAnswerAggregateId, unitOfWork);
        QuizAnswer newQuizAnswer = quizAnswerFactory.createQuizAnswerCopy(oldQuizAnswer);

        QuestionAnswer questionAnswer = findQuestionAnswer(newQuizAnswer, questionAggregateId);
        if (questionAnswer == null) {
            throw new QuizzesFull2Exception(QuizzesFull2ErrorMessage.QUESTION_NOT_IN_QUIZ_ANSWER);
        }

        questionAnswer.setOptionSequenceChoice(optionSequenceChoice);
        questionAnswer.setOptionKey(optionKey);
        questionAnswer.setCorrect(Objects.equals(optionKey, questionAnswer.getCorrectOptionKey()));
        questionAnswer.setTimeTaken(timeTaken);

        LocalDateTime answerTime = DateHandler.now();
        unitOfWorkService.registerChanged(newQuizAnswer, unitOfWork);
        unitOfWorkService.registerEvent(new QuizAnswerQuestionAnswerEvent(
                newQuizAnswer.getAggregateId(), questionAggregateId,
                newQuizAnswer.getQuiz().getQuizAggregateId(),
                newQuizAnswer.getStudent().getUserAggregateId(),
                questionAnswer.getCorrect(), answerTime), unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void concludeQuiz(Integer quizAnswerAggregateId, UnitOfWork unitOfWork) {
        QuizAnswer oldQuizAnswer = (QuizAnswer) unitOfWorkService.aggregateLoadAndRegisterRead(
                quizAnswerAggregateId, unitOfWork);
        QuizAnswer newQuizAnswer = quizAnswerFactory.createQuizAnswerCopy(oldQuizAnswer);

        newQuizAnswer.setCompleted(true);

        unitOfWorkService.registerChanged(newQuizAnswer, unitOfWork);
    }

    private static QuestionAnswer findQuestionAnswer(QuizAnswer quizAnswer, Integer questionAggregateId) {
        return quizAnswer.getQuestionAnswers().stream()
                .filter(questionAnswer -> questionAggregateId.equals(questionAnswer.getQuestionAggregateId()))
                .findFirst()
                .orElse(null);
    }
}

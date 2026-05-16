package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.QuizAnswerQuestionAnswerEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullException;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.aggregate.QuestionAnswer;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.aggregate.QuizAnswer;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.aggregate.QuizAnswerCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.aggregate.QuizAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.aggregate.QuizAnswerFactory;

import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.UNIQUE_QUIZ_ANSWER_PER_STUDENT;

@Service
public class QuizAnswerService {

    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private QuizAnswerFactory quizAnswerFactory;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;
    private final QuizAnswerCustomRepository quizAnswerRepository;

    public QuizAnswerService(UnitOfWorkService unitOfWorkService, QuizAnswerCustomRepository quizAnswerRepository) {
        this.unitOfWorkService = unitOfWorkService;
        this.quizAnswerRepository = quizAnswerRepository;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public QuizAnswerDto getQuizAnswerById(Integer quizAnswerAggregateId, UnitOfWork unitOfWork) {
        return quizAnswerFactory.createQuizAnswerDto(
                (QuizAnswer) unitOfWorkService.aggregateLoadAndRegisterRead(quizAnswerAggregateId, unitOfWork));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public QuizAnswerDto createQuizAnswer(Integer quizAggregateId, Long quizVersion,
                                          Integer userAggregateId, Long userVersion,
                                          String userName, String userUsername,
                                          Integer executionAggregateId, Long executionVersion,
                                          UnitOfWork unitOfWork) {
        // [P3] UNIQUE_QUIZ_ANSWER_PER_STUDENT
        if (quizAnswerRepository.findByQuizAggregateIdAndUserAggregateId(quizAggregateId, userAggregateId).isPresent()) {
            throw new QuizzesFullException(UNIQUE_QUIZ_ANSWER_PER_STUDENT);
        }

        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        QuizAnswer quizAnswer = quizAnswerFactory.createQuizAnswer(aggregateId,
                quizAggregateId, quizVersion, userAggregateId, userVersion,
                userName, userUsername, executionAggregateId, executionVersion);
        unitOfWorkService.registerChanged(quizAnswer, unitOfWork);
        return quizAnswerFactory.createQuizAnswerDto(quizAnswer);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void answerQuestion(Integer quizAnswerAggregateId, Integer questionAggregateId,
                                Long questionVersion, Integer optionKey, Integer timeTaken,
                                UnitOfWork unitOfWork) {
        QuizAnswer old = (QuizAnswer) unitOfWorkService.aggregateLoadAndRegisterRead(quizAnswerAggregateId, unitOfWork);
        QuizAnswer newQA = quizAnswerFactory.createQuizAnswerCopy(old);
        newQA.addQuestionAnswer(new QuestionAnswer(questionAggregateId, questionVersion, optionKey, optionKey, null, timeTaken));
        unitOfWorkService.registerChanged(newQA, unitOfWork);
        unitOfWorkService.registerEvent(
                new QuizAnswerQuestionAnswerEvent(quizAnswerAggregateId, newQA.getQuizAggregateId(), newQA.getUserAggregateId()),
                unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void concludeQuiz(Integer quizAnswerAggregateId, UnitOfWork unitOfWork) {
        QuizAnswer old = (QuizAnswer) unitOfWorkService.aggregateLoadAndRegisterRead(quizAnswerAggregateId, unitOfWork);
        QuizAnswer newQA = quizAnswerFactory.createQuizAnswerCopy(old);
        newQA.setCompleted(true);
        unitOfWorkService.registerChanged(newQA, unitOfWork);
    }
}

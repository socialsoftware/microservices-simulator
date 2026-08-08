package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2ErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2Exception;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate.QuizAnswer;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate.QuizAnswerCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate.QuizAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate.QuizAnswerFactory;

@Service
public class QuizAnswerService {
    private final QuizAnswerCustomRepository quizAnswerCustomRepository;
    private final QuizAnswerFactory quizAnswerFactory;
    private final UnitOfWorkService unitOfWorkService;

    public QuizAnswerService(QuizAnswerCustomRepository quizAnswerCustomRepository,
                             QuizAnswerFactory quizAnswerFactory,
                             UnitOfWorkService unitOfWorkService) {
        this.quizAnswerCustomRepository = quizAnswerCustomRepository;
        this.quizAnswerFactory = quizAnswerFactory;
        this.unitOfWorkService = unitOfWorkService;
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
}

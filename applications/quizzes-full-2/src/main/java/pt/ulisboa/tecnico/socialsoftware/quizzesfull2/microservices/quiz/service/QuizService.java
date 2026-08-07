package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.Quiz;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizFactory;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuizService {
    private final QuizCustomRepository quizCustomRepository;
    private final QuizFactory quizFactory;
    private final UnitOfWorkService unitOfWorkService;

    public QuizService(QuizCustomRepository quizCustomRepository,
                       QuizFactory quizFactory,
                       UnitOfWorkService unitOfWorkService) {
        this.quizCustomRepository = quizCustomRepository;
        this.quizFactory = quizFactory;
        this.unitOfWorkService = unitOfWorkService;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public QuizDto getQuizById(Integer quizAggregateId, UnitOfWork unitOfWork) {
        return quizFactory.createQuizDto(
                (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(quizAggregateId, unitOfWork));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<QuizDto> getQuizzesForExecution(Integer executionAggregateId, UnitOfWork unitOfWork) {
        return quizCustomRepository.findQuizIdsByExecution(executionAggregateId).stream()
                .map(quizAggregateId -> quizFactory.createQuizDto(
                        (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(quizAggregateId, unitOfWork)))
                .collect(Collectors.toList());
    }
}

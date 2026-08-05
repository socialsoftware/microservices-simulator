package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.Question;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.QuestionCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.QuestionFactory;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuestionService {
    private final QuestionCustomRepository questionCustomRepository;
    private final QuestionFactory questionFactory;
    private final UnitOfWorkService unitOfWorkService;

    public QuestionService(QuestionCustomRepository questionCustomRepository,
                           QuestionFactory questionFactory,
                           UnitOfWorkService unitOfWorkService) {
        this.questionCustomRepository = questionCustomRepository;
        this.questionFactory = questionFactory;
        this.unitOfWorkService = unitOfWorkService;
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
}

package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.Quiz;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizQuestion;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizQuestionDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuizService {
    private final QuizCustomRepository quizCustomRepository;
    private final QuizFactory quizFactory;
    private final UnitOfWorkService unitOfWorkService;
    private final AggregateIdGeneratorService aggregateIdGeneratorService;

    public QuizService(QuizCustomRepository quizCustomRepository,
                       QuizFactory quizFactory,
                       UnitOfWorkService unitOfWorkService,
                       AggregateIdGeneratorService aggregateIdGeneratorService) {
        this.quizCustomRepository = quizCustomRepository;
        this.quizFactory = quizFactory;
        this.unitOfWorkService = unitOfWorkService;
        this.aggregateIdGeneratorService = aggregateIdGeneratorService;
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

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public QuizDto createQuiz(QuizDto quizDto, ExecutionDto executionDto, List<QuizQuestionDto> questions,
                              UnitOfWork unitOfWork) {
        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        Quiz quiz = quizFactory.createQuiz(aggregateId, executionDto.getAggregateId(), executionDto.getVersion(),
                quizDto.getTitle(), DateHandler.now(), quizDto.getAvailableDate(), quizDto.getConclusionDate(),
                quizDto.getResultsDate(), quizDto.getQuizType());

        questions.forEach(questionDto -> quiz.addQuestion(toQuizQuestion(questionDto)));

        unitOfWorkService.registerChanged(quiz, unitOfWork);
        return quizFactory.createQuizDto(quiz);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void updateQuiz(Integer quizAggregateId, String title, LocalDateTime availableDate,
                           LocalDateTime conclusionDate, LocalDateTime resultsDate,
                           List<QuizQuestionDto> questions, UnitOfWork unitOfWork) {
        Quiz oldQuiz = (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(quizAggregateId, unitOfWork);
        Quiz newQuiz = quizFactory.createQuizCopy(oldQuiz);

        newQuiz.setTitle(title);
        newQuiz.setAvailableDate(availableDate);
        newQuiz.setConclusionDate(conclusionDate);
        newQuiz.setResultsDate(resultsDate);
        newQuiz.setQuestions(questions.stream().map(QuizService::toQuizQuestion).collect(Collectors.toList()));

        unitOfWorkService.registerChanged(newQuiz, unitOfWork);
    }

    private static QuizQuestion toQuizQuestion(QuizQuestionDto questionDto) {
        return new QuizQuestion(questionDto.getQuestionAggregateId(), questionDto.getQuestionVersion(),
                questionDto.getTitle(), questionDto.getContent());
    }
}

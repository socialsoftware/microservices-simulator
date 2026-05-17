package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.InvalidateQuizEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.Quiz;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.QuizCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.QuizExecution;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.QuizFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.QuizQuestion;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.QuizType;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class QuizService {

    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private QuizFactory quizFactory;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;
    private final QuizCustomRepository quizRepository;

    public QuizService(UnitOfWorkService unitOfWorkService, QuizCustomRepository quizRepository) {
        this.unitOfWorkService = unitOfWorkService;
        this.quizRepository = quizRepository;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public QuizDto getQuizById(Integer quizAggregateId, UnitOfWork unitOfWork) {
        return quizFactory.createQuizDto(
                (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(quizAggregateId, unitOfWork));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public QuizDto createQuiz(String title, LocalDateTime availableDate, LocalDateTime conclusionDate,
                               LocalDateTime resultsDate, QuizType quizType,
                               QuizExecution quizExecution, Set<QuizQuestion> questions,
                               UnitOfWork unitOfWork) {
        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        Quiz quiz = quizFactory.createQuiz(aggregateId, title, availableDate, conclusionDate,
                resultsDate, quizType, quizExecution, questions);
        unitOfWorkService.registerChanged(quiz, unitOfWork);
        return quizFactory.createQuizDto(quiz);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void updateQuiz(Integer quizAggregateId, LocalDateTime availableDate, LocalDateTime conclusionDate,
                            LocalDateTime resultsDate, Set<QuizQuestion> questions,
                            UnitOfWork unitOfWork) {
        Quiz oldQuiz = (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(quizAggregateId, unitOfWork);
        Quiz newQuiz = quizFactory.createQuizCopy(oldQuiz);
        newQuiz.setAvailableDate(availableDate);
        newQuiz.setConclusionDate(conclusionDate);
        newQuiz.setResultsDate(resultsDate);
        if (questions != null) {
            newQuiz.setQuestions(questions);
        }
        unitOfWorkService.registerChanged(newQuiz, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void updateQuestionInQuiz(Integer quizAggregateId, Integer questionAggregateId,
                                     String title, String content, UnitOfWork unitOfWork) {
        Quiz oldQuiz = (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(quizAggregateId, unitOfWork);
        Quiz newQuiz = quizFactory.createQuizCopy(oldQuiz);
        for (QuizQuestion q : newQuiz.getQuestions()) {
            if (q.getQuestionAggregateId().equals(questionAggregateId)) {
                q.setTitle(title);
                q.setContent(content);
                break;
            }
        }
        unitOfWorkService.registerChanged(newQuiz, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void removeQuestionFromQuiz(Integer quizAggregateId, Integer questionAggregateId,
                                       UnitOfWork unitOfWork) {
        Quiz oldQuiz = (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(quizAggregateId, unitOfWork);
        Quiz newQuiz = quizFactory.createQuizCopy(oldQuiz);
        newQuiz.getQuestions().removeIf(q -> q.getQuestionAggregateId().equals(questionAggregateId));
        newQuiz.remove();
        unitOfWorkService.registerChanged(newQuiz, unitOfWork);
        unitOfWorkService.registerEvent(new InvalidateQuizEvent(quizAggregateId), unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void invalidateQuiz(Integer quizAggregateId, UnitOfWork unitOfWork) {
        Quiz oldQuiz = (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(quizAggregateId, unitOfWork);
        Quiz newQuiz = quizFactory.createQuizCopy(oldQuiz);
        newQuiz.remove();
        unitOfWorkService.registerChanged(newQuiz, unitOfWork);
        unitOfWorkService.registerEvent(new InvalidateQuizEvent(quizAggregateId), unitOfWork);
    }
}

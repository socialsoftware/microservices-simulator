package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.dao.CannotAcquireLockException;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.publish.QuizUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.publish.QuizDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.QuizType;

@Service
public class QuizService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    private final QuizRepository quizRepository;

    @Autowired
    private QuizFactory quizFactory;

    public QuizService(UnitOfWorkService unitOfWorkService, QuizRepository quizRepository) {
        this.unitOfWorkService = unitOfWorkService;
        this.quizRepository = quizRepository;
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public QuizDto createQuiz(QuizDto quizDto, UnitOfWork unitOfWork) {
        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        Quiz quiz = quizFactory.createQuiz(aggregateId, quizDto);
        unitOfWorkService.registerChanged(quiz, unitOfWork);
        return quizFactory.createQuizDto(quiz);
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public QuizDto getQuizById(Integer aggregateId, UnitOfWork unitOfWork) {
        return quizFactory.createQuizDto((Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork));
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public QuizDto updateQuiz(Integer aggregateId, QuizDto quizDto, UnitOfWork unitOfWork) {
        Quiz oldQuiz = (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        Quiz newQuiz = quizFactory.createQuizFromExisting(oldQuiz);
        newQuiz.setTitle(quizDto.getTitle());
        newQuiz.setCreationDate(quizDto.getCreationDate());
        newQuiz.setAvailableDate(quizDto.getAvailableDate());
        newQuiz.setConclusionDate(quizDto.getConclusionDate());
        newQuiz.setResultsDate(quizDto.getResultsDate());
        unitOfWorkService.registerChanged(newQuiz, unitOfWork);
        unitOfWorkService.registerEvent(new QuizUpdatedEvent(newQuiz.getAggregateId(), newQuiz.getTitle(), newQuiz.getCreationDate(), newQuiz.getAvailableDate(), newQuiz.getConclusionDate(), newQuiz.getResultsDate()), unitOfWork);
        return quizFactory.createQuizDto(newQuiz);
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteQuiz(Integer aggregateId, UnitOfWork unitOfWork) {
        Quiz oldQuiz = (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        Quiz newQuiz = quizFactory.createQuizFromExisting(oldQuiz);
        newQuiz.remove();
        unitOfWorkService.registerChanged(newQuiz, unitOfWork);
        unitOfWorkService.registerEvent(new QuizDeletedEvent(newQuiz.getAggregateId()), unitOfWork);
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<QuizDto> searchQuizs(String title, QuizType quizType, UnitOfWork unitOfWork) {
        Set<Integer> aggregateIds = quizRepository.findAll().stream()
                .filter(entity -> {
                    if (title != null) {
                        if (!entity.getTitle().equals(title)) {
                            return false;
                        }
                    }
                    if (quizType != null) {
                        if (!entity.getQuizType().equals(quizType)) {
                            return false;
                        }
                                            }
                    return true;
                })
                .map(Quiz::getAggregateId)
                .collect(Collectors.toSet());
        return aggregateIds.stream()
                .map(id -> (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .map(quizFactory::createQuizDto)
                .collect(Collectors.toList());
    }

}

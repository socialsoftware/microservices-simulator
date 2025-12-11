package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.service;

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
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.*;

@Service
public class AnswerService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    private final AnswerRepository answerRepository;

    @Autowired
    private AnswerFactory answerFactory;

    public AnswerService(UnitOfWorkService unitOfWorkService, AnswerRepository answerRepository) {
        this.unitOfWorkService = unitOfWorkService;
        this.answerRepository = answerRepository;
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public AnswerDto createAnswer(AnswerDto answerDto, UnitOfWork unitOfWork) {
        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        Answer answer = answerFactory.createAnswer(aggregateId, answerDto);
        unitOfWorkService.registerChanged(answer, unitOfWork);
        return answerFactory.createAnswerDto(answer);
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public AnswerDto getAnswerById(Integer aggregateId, UnitOfWork unitOfWork) {
        return answerFactory.createAnswerDto((Answer) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork));
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public AnswerDto updateAnswer(Integer aggregateId, AnswerDto answerDto, UnitOfWork unitOfWork) {
        Answer oldAnswer = (Answer) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        Answer newAnswer = answerFactory.createAnswerFromExisting(oldAnswer);
        newAnswer.setCreationDate(answerDto.getCreationDate());
        newAnswer.setAnswerDate(answerDto.getAnswerDate());
        newAnswer.setCompleted(answerDto.getCompleted());
        unitOfWorkService.registerChanged(newAnswer, unitOfWork);
        return answerFactory.createAnswerDto(newAnswer);
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteAnswer(Integer aggregateId, UnitOfWork unitOfWork) {
        Answer oldAnswer = (Answer) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        Answer newAnswer = answerFactory.createAnswerFromExisting(oldAnswer);
        newAnswer.remove();
        unitOfWorkService.registerChanged(newAnswer, unitOfWork);
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<AnswerDto> getAllAnswers(UnitOfWork unitOfWork) {
        Set<Integer> aggregateIds = answerRepository.findAll().stream()
                .map(Answer::getAggregateId)
                .collect(Collectors.toSet());
        return aggregateIds.stream()
                .map(id -> (Answer) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .map(answerFactory::createAnswerDto)
                .collect(Collectors.toList());
    }

}

package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.service;

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
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish.ExecutionUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish.ExecutionDeletedEvent;

@Service
public class ExecutionService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    private final ExecutionRepository executionRepository;

    @Autowired
    private ExecutionFactory executionFactory;

    public ExecutionService(UnitOfWorkService unitOfWorkService, ExecutionRepository executionRepository) {
        this.unitOfWorkService = unitOfWorkService;
        this.executionRepository = executionRepository;
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ExecutionDto createExecution(ExecutionDto executionDto, UnitOfWork unitOfWork) {
        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        Execution execution = executionFactory.createExecution(aggregateId, executionDto);
        unitOfWorkService.registerChanged(execution, unitOfWork);
        return executionFactory.createExecutionDto(execution);
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ExecutionDto getExecutionById(Integer aggregateId, UnitOfWork unitOfWork) {
        return executionFactory.createExecutionDto((Execution) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork));
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ExecutionDto updateExecution(Integer aggregateId, ExecutionDto executionDto, UnitOfWork unitOfWork) {
        Execution oldExecution = (Execution) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        Execution newExecution = executionFactory.createExecutionFromExisting(oldExecution);
        newExecution.setAcronym(executionDto.getAcronym());
        newExecution.setAcademicTerm(executionDto.getAcademicTerm());
        newExecution.setEndDate(executionDto.getEndDate());
        unitOfWorkService.registerChanged(newExecution, unitOfWork);
        unitOfWorkService.registerEvent(new ExecutionUpdatedEvent(newExecution.getAggregateId(), newExecution.getAcronym(), newExecution.getAcademicTerm(), newExecution.getEndDate()), unitOfWork);
        return executionFactory.createExecutionDto(newExecution);
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteExecution(Integer aggregateId, UnitOfWork unitOfWork) {
        Execution oldExecution = (Execution) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        Execution newExecution = executionFactory.createExecutionFromExisting(oldExecution);
        newExecution.remove();
        unitOfWorkService.registerChanged(newExecution, unitOfWork);
        unitOfWorkService.registerEvent(new ExecutionDeletedEvent(newExecution.getAggregateId()), unitOfWork);
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<ExecutionDto> searchExecutions(String acronym, String academicTerm, Integer courseAggregateId, UnitOfWork unitOfWork) {
        Set<Integer> aggregateIds = executionRepository.findAll().stream()
                .filter(entity -> {
                    if (acronym != null) {
                        if (!entity.getAcronym().equals(acronym)) {
                            return false;
                        }
                    }
                    if (academicTerm != null) {
                        if (!entity.getAcademicTerm().equals(academicTerm)) {
                            return false;
                        }
                    }
                    if (courseAggregateId != null) {
                        if (!entity.getCourse().getCourseAggregateId().equals(courseAggregateId)) {
                            return false;
                        }
                                            }
                    return true;
                })
                .map(Execution::getAggregateId)
                .collect(Collectors.toSet());
        return aggregateIds.stream()
                .map(id -> (Execution) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .map(executionFactory::createExecutionDto)
                .collect(Collectors.toList());
    }

}

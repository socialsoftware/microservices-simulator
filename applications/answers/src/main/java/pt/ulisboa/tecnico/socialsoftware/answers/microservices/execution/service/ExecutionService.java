package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.repository.*;
import java.util.List;
import java.util.Set;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;
import java.sql.SQLException;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Isolation;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersException;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersErrorMessage;

@Service
@Transactional
public class ExecutionService {
    @Autowired
    private ExecutionRepository executionRepository;
    @Autowired
    private UnitOfWorkService unitOfWorkService;
    @Autowired
    private ExecutionFactory executionFactory;

    public Execution createExecution(ExecutionDto executionDto) {
        // TODO: Implement createExecution method
        return null; // Placeholder
    }

    public Optional<Execution> findExecutionById(Integer id) {
        // TODO: Implement findExecutionById method
        return null; // Placeholder
    }

    public Execution updateExecution(Integer id, ExecutionDto executionDto) {
        // TODO: Implement updateExecution method
        return null; // Placeholder
    }

    public void deleteExecution(Integer id) {
        // TODO: Implement deleteExecution method
    }

    public List<Execution> findAllExecutions() {
        // TODO: Implement findAllExecutions method
        return null; // Placeholder
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
        backoff = @Backoff(
            delayExpression = "${retry.db.delay}",
            multiplierExpression = "${retry.db.multiplier}"
        ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Execution removeUser(Integer executionAggregateId, Integer userAggregateId, Integer userVersion, Object unitOfWork) {
        Execution oldExecution = (Execution) unitOfWorkService.aggregateLoadAndRegisterRead(executionAggregateId, unitOfWork);
        Execution newExecution = executionFactory.createExecutionFromExisting(oldExecution);
        newExecution.findStudent(userAggregateId).setState(Aggregate.AggregateState.INACTIVE);
        unitOfWorkService.registerChanged(newExecution, unitOfWork);
        unitOfWorkService.registerEvent(new DisenrollStudentFromExecutionEvent(executionAggregateId, userAggregateId), unitOfWork);
        return newExecution;
    }

    public Object anonymizeStudent(Integer executionAggregateId, Integer userAggregateId, Object unitOfWork) {
        // TODO: Implement anonymizeStudent method
        return null; // Placeholder
    }

    public Object updateStudentName(Integer executionAggregateId, Integer userAggregateId, String newName, Object unitOfWork) {
        // TODO: Implement updateStudentName method
        return null; // Placeholder
    }

    // Additional CRUD utility methods can be added here
}

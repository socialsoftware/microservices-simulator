package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.Execution;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.ExecutionCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.ExecutionFactory;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExecutionService {
    private final ExecutionCustomRepository executionCustomRepository;
    private final ExecutionFactory executionFactory;
    private final UnitOfWorkService unitOfWorkService;

    public ExecutionService(ExecutionCustomRepository executionCustomRepository,
                            ExecutionFactory executionFactory,
                            UnitOfWorkService unitOfWorkService) {
        this.executionCustomRepository = executionCustomRepository;
        this.executionFactory = executionFactory;
        this.unitOfWorkService = unitOfWorkService;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ExecutionDto getExecutionById(Integer executionAggregateId, UnitOfWork unitOfWork) {
        return executionFactory.createExecutionDto(
                (Execution) unitOfWorkService.aggregateLoadAndRegisterRead(executionAggregateId, unitOfWork));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<ExecutionDto> getExecutions(UnitOfWork unitOfWork) {
        return executionCustomRepository.findAllExecutionIds().stream()
                .map(executionAggregateId -> executionFactory.createExecutionDto(
                        (Execution) unitOfWorkService.aggregateLoadAndRegisterRead(executionAggregateId, unitOfWork)))
                .collect(Collectors.toList());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<ExecutionDto> getUserExecutions(Integer userAggregateId, UnitOfWork unitOfWork) {
        return executionCustomRepository.findExecutionIdsByStudent(userAggregateId).stream()
                .map(executionAggregateId -> executionFactory.createExecutionDto(
                        (Execution) unitOfWorkService.aggregateLoadAndRegisterRead(executionAggregateId, unitOfWork)))
                .collect(Collectors.toList());
    }
}

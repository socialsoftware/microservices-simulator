package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionCourseDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionUserDto;
import java.time.LocalDateTime;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish.ExecutionDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish.ExecutionUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersException;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos.CreateExecutionRequestDto;


@Service
@Transactional
public class ExecutionService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService<UnitOfWork> unitOfWorkService;

    @Autowired
    private ExecutionRepository executionRepository;

    @Autowired
    private ExecutionFactory executionFactory;

    public ExecutionService() {}

    public ExecutionDto createExecution(CreateExecutionRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            ExecutionDto executionDto = new ExecutionDto();
            executionDto.setAcronym(createRequest.getAcronym());
            executionDto.setAcademicTerm(createRequest.getAcademicTerm());
            executionDto.setEndDate(createRequest.getEndDate());
            if (createRequest.getCourse() != null) {
                ExecutionCourseDto courseDto = new ExecutionCourseDto();
                courseDto.setAggregateId(createRequest.getCourse().getAggregateId());
                courseDto.setVersion(createRequest.getCourse().getVersion());
                courseDto.setState(createRequest.getCourse().getState());
                executionDto.setCourse(courseDto);
            }
            if (createRequest.getUsers() != null) {
                executionDto.setUsers(createRequest.getUsers().stream().map(srcDto -> {
                    ExecutionUserDto projDto = new ExecutionUserDto();
                    projDto.setAggregateId(srcDto.getAggregateId());
                    projDto.setVersion(srcDto.getVersion());
                    projDto.setState(srcDto.getState());
                    return projDto;
                }).collect(Collectors.toSet()));
            }
            
            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Execution execution = executionFactory.createExecution(aggregateId, executionDto);
            unitOfWorkService.registerChanged(execution, unitOfWork);
            return executionFactory.createExecutionDto(execution);
        } catch (Exception e) {
            throw new AnswersException("Error creating execution: " + e.getMessage());
        }
    }

    public ExecutionDto getExecutionById(Integer id, UnitOfWork unitOfWork) {
        try {
            Execution execution = (Execution) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return executionFactory.createExecutionDto(execution);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error retrieving execution: " + e.getMessage());
        }
    }

    public List<ExecutionDto> getAllExecutions(UnitOfWork unitOfWork) {
        try {
            Set<Integer> aggregateIds = executionRepository.findAll().stream()
                .map(Execution::getAggregateId)
                .collect(Collectors.toSet());

            return aggregateIds.stream()
                .map(id -> (Execution) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .map(executionFactory::createExecutionDto)
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new AnswersException("Error retrieving all executions: " + e.getMessage());
        }
    }

    public ExecutionDto updateExecution(ExecutionDto executionDto, UnitOfWork unitOfWork) {
        try {
            Integer id = executionDto.getAggregateId();
            Execution execution = (Execution) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            if (executionDto.getAcronym() != null) {
                execution.setAcronym(executionDto.getAcronym());
            }
            if (executionDto.getAcademicTerm() != null) {
                execution.setAcademicTerm(executionDto.getAcademicTerm());
            }
            if (executionDto.getEndDate() != null) {
                execution.setEndDate(executionDto.getEndDate());
            }

            unitOfWorkService.registerChanged(execution, unitOfWork);
            unitOfWorkService.registerEvent(new ExecutionUpdatedEvent(execution.getAggregateId(), execution.getAcronym(), execution.getAcademicTerm(), execution.getEndDate()), unitOfWork);
            return executionFactory.createExecutionDto(execution);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error updating execution: " + e.getMessage());
        }
    }

    public void deleteExecution(Integer id, UnitOfWork unitOfWork) {
        try {
            Execution execution = (Execution) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            execution.remove();
            unitOfWorkService.registerChanged(execution, unitOfWork);
            unitOfWorkService.registerEvent(new ExecutionDeletedEvent(execution.getAggregateId()), unitOfWork);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error deleting execution: " + e.getMessage());
        }
    }






}
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
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish.ExecutionDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish.ExecutionUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish.ExecutionCourseDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish.ExecutionCourseUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish.ExecutionUserDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish.ExecutionUserUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish.ExecutionUserRemovedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish.ExecutionUserUpdatedEvent;
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
            Execution oldExecution = (Execution) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Execution newExecution = executionFactory.createExecutionFromExisting(oldExecution);
            if (executionDto.getAcronym() != null) {
                newExecution.setAcronym(executionDto.getAcronym());
            }
            if (executionDto.getAcademicTerm() != null) {
                newExecution.setAcademicTerm(executionDto.getAcademicTerm());
            }
            if (executionDto.getEndDate() != null) {
                newExecution.setEndDate(executionDto.getEndDate());
            }

            unitOfWorkService.registerChanged(newExecution, unitOfWork);
            ExecutionUpdatedEvent event = new ExecutionUpdatedEvent(newExecution.getAggregateId(), newExecution.getAcronym(), newExecution.getAcademicTerm(), newExecution.getEndDate());
            event.setPublisherAggregateVersion(newExecution.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return executionFactory.createExecutionDto(newExecution);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error updating execution: " + e.getMessage());
        }
    }

    public void deleteExecution(Integer id, UnitOfWork unitOfWork) {
        try {
            Execution oldExecution = (Execution) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Execution newExecution = executionFactory.createExecutionFromExisting(oldExecution);
            newExecution.remove();
            unitOfWorkService.registerChanged(newExecution, unitOfWork);
            unitOfWorkService.registerEvent(new ExecutionDeletedEvent(newExecution.getAggregateId()), unitOfWork);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error deleting execution: " + e.getMessage());
        }
    }

    public ExecutionUserDto addExecutionUser(Integer executionId, Integer userAggregateId, ExecutionUserDto ExecutionUserDto, UnitOfWork unitOfWork) {
        try {
            Execution oldExecution = (Execution) unitOfWorkService.aggregateLoadAndRegisterRead(executionId, unitOfWork);
            Execution newExecution = executionFactory.createExecutionFromExisting(oldExecution);
            ExecutionUser element = new ExecutionUser(ExecutionUserDto);
            newExecution.getUsers().add(element);
            unitOfWorkService.registerChanged(newExecution, unitOfWork);
            return ExecutionUserDto;
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error adding ExecutionUser: " + e.getMessage());
        }
    }

    public List<ExecutionUserDto> addExecutionUsers(Integer executionId, List<ExecutionUserDto> ExecutionUserDtos, UnitOfWork unitOfWork) {
        try {
            Execution oldExecution = (Execution) unitOfWorkService.aggregateLoadAndRegisterRead(executionId, unitOfWork);
            Execution newExecution = executionFactory.createExecutionFromExisting(oldExecution);
            ExecutionUserDtos.forEach(dto -> {
                ExecutionUser element = new ExecutionUser(dto);
                newExecution.getUsers().add(element);
            });
            unitOfWorkService.registerChanged(newExecution, unitOfWork);
            return ExecutionUserDtos;
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error adding ExecutionUsers: " + e.getMessage());
        }
    }

    public ExecutionUserDto getExecutionUser(Integer executionId, Integer userAggregateId, UnitOfWork unitOfWork) {
        try {
            Execution execution = (Execution) unitOfWorkService.aggregateLoadAndRegisterRead(executionId, unitOfWork);
            ExecutionUser element = execution.getUsers().stream()
                .filter(item -> item.getUserAggregateId() != null &&
                               item.getUserAggregateId().equals(userAggregateId))
                .findFirst()
                .orElseThrow(() -> new AnswersException("ExecutionUser not found"));
            return element.buildDto();
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error retrieving ExecutionUser: " + e.getMessage());
        }
    }

    public void removeExecutionUser(Integer executionId, Integer userAggregateId, UnitOfWork unitOfWork) {
        try {
            Execution oldExecution = (Execution) unitOfWorkService.aggregateLoadAndRegisterRead(executionId, unitOfWork);
            Execution newExecution = executionFactory.createExecutionFromExisting(oldExecution);
            newExecution.getUsers().removeIf(item ->
                item.getUserAggregateId() != null &&
                item.getUserAggregateId().equals(userAggregateId)
            );
            unitOfWorkService.registerChanged(newExecution, unitOfWork);
            ExecutionUserRemovedEvent event = new ExecutionUserRemovedEvent(executionId, userAggregateId);
            event.setPublisherAggregateVersion(newExecution.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error removing ExecutionUser: " + e.getMessage());
        }
    }

    public ExecutionUserDto updateExecutionUser(Integer executionId, Integer userAggregateId, ExecutionUserDto ExecutionUserDto, UnitOfWork unitOfWork) {
        try {
            Execution oldExecution = (Execution) unitOfWorkService.aggregateLoadAndRegisterRead(executionId, unitOfWork);
            Execution newExecution = executionFactory.createExecutionFromExisting(oldExecution);
            ExecutionUser element = newExecution.getUsers().stream()
                .filter(item -> item.getUserAggregateId() != null &&
                               item.getUserAggregateId().equals(userAggregateId))
                .findFirst()
                .orElseThrow(() -> new AnswersException("ExecutionUser not found"));

            unitOfWorkService.registerChanged(newExecution, unitOfWork);
            ExecutionUserUpdatedEvent event = new ExecutionUserUpdatedEvent(executionId, element.getUserAggregateId(), element.getUserVersion());
            event.setPublisherAggregateVersion(newExecution.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return element.buildDto();
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error updating ExecutionUser: " + e.getMessage());
        }
    }






}
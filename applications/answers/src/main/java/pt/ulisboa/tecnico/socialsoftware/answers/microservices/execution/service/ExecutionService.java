package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionCourseDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionUserDto;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.answers.events.ExecutionDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.events.ExecutionUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.events.*;
import pt.ulisboa.tecnico.socialsoftware.answers.events.ExecutionUserRemovedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.events.ExecutionUserUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersException;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.coordination.webapi.requestDtos.CreateExecutionRequestDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.aggregate.Course;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.aggregate.User;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.UserDto;


@Service
@Transactional(noRollbackFor = AnswersException.class)
public class ExecutionService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService unitOfWorkService;

    @Autowired
    private ExecutionRepository executionRepository;

    @Autowired
    private ExecutionFactory executionFactory;

    @Autowired
    private ExecutionServiceExtension extension;

    public ExecutionService() {}

    public ExecutionDto createExecution(CreateExecutionRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            ExecutionDto executionDto = new ExecutionDto();
            executionDto.setAcronym(createRequest.getAcronym());
            executionDto.setAcademicTerm(createRequest.getAcademicTerm());
            executionDto.setEndDate(createRequest.getEndDate());
            if (createRequest.getCourse() != null) {
                Course refSource = (Course) unitOfWorkService.aggregateLoadAndRegisterRead(createRequest.getCourse().getAggregateId(), unitOfWork);
                CourseDto refSourceDto = new CourseDto(refSource);
                ExecutionCourseDto courseDto = new ExecutionCourseDto();
                courseDto.setAggregateId(refSourceDto.getAggregateId());
                courseDto.setVersion(refSourceDto.getVersion());
                courseDto.setState(refSourceDto.getState() != null ? refSourceDto.getState().name() : null);
                courseDto.setName(refSourceDto.getName());
                courseDto.setType(refSourceDto.getType());
                executionDto.setCourse(courseDto);
            }
            if (createRequest.getUsers() != null) {
                executionDto.setUsers(createRequest.getUsers().stream().map(reqDto -> {
                    User refItem = (User) unitOfWorkService.aggregateLoadAndRegisterRead(reqDto.getAggregateId(), unitOfWork);
                    UserDto refItemDto = new UserDto(refItem);
                    ExecutionUserDto projDto = new ExecutionUserDto();
                    projDto.setAggregateId(refItemDto.getAggregateId());
                    projDto.setVersion(refItemDto.getVersion());
                    projDto.setState(refItemDto.getState() != null ? refItemDto.getState().name() : null);
                    projDto.setName(refItemDto.getName());
                    projDto.setUsername(refItemDto.getUsername());
                    projDto.setActive(refItemDto.getActive());
                    return projDto;
                }).collect(Collectors.toSet()));
            }

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Execution execution = executionFactory.createExecution(aggregateId, executionDto);
            unitOfWorkService.registerChanged(execution, unitOfWork);
            return executionFactory.createExecutionDto(execution);
        } catch (AnswersException e) {
            throw e;
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
                .map(id -> {
                    try {
                        return (Execution) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .map(executionFactory::createExecutionDto)
                .collect(Collectors.toList());
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error retrieving execution: " + e.getMessage());
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

            unitOfWorkService.registerChanged(newExecution, unitOfWork);            ExecutionUpdatedEvent event = new ExecutionUpdatedEvent(newExecution.getAggregateId(), newExecution.getAcronym(), newExecution.getAcademicTerm(), newExecution.getEndDate());
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
            unitOfWorkService.registerChanged(newExecution, unitOfWork);            unitOfWorkService.registerEvent(new ExecutionDeletedEvent(newExecution.getAggregateId()), unitOfWork);
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
            element.setExecution(newExecution);
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
                element.setExecution(newExecution);
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
            ExecutionUserUpdatedEvent event = new ExecutionUserUpdatedEvent(executionId, element.getUserAggregateId(), element.getUserVersion(), element.getUserName(), element.getUserUsername(), element.getUserActive());
            event.setPublisherAggregateVersion(newExecution.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return element.buildDto();
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error updating ExecutionUser: " + e.getMessage());
        }
    }


    public Execution handleUserUpdatedEvent(Integer aggregateId, Integer userAggregateId, Integer userVersion, String userName, String userUsername, Boolean userActive, UnitOfWork unitOfWork) {
        try {
            Execution oldExecution = (Execution) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
            Execution newExecution = executionFactory.createExecutionFromExisting(oldExecution);



            unitOfWorkService.registerChanged(newExecution, unitOfWork);

        unitOfWorkService.registerEvent(
            new ExecutionUserUpdatedEvent(
                    newExecution.getAggregateId(),
                    userAggregateId,
                    userVersion,
                    userName,
                    userUsername,
                    userActive
            ),
            unitOfWork
        );

            return newExecution;
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error handling UserUpdatedEvent execution: " + e.getMessage());
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public java.util.List<ExecutionDto> getAllNonDeletedExecutions(UnitOfWork unitOfWork) {
        try {
            Set<Integer> aggregateIds = executionRepository.findCourseExecutionIdsOfAllNonDeletedForSaga();
            return aggregateIds.stream()
                .map(id -> {
                    try {
                        return (Execution) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .map(executionFactory::createExecutionDto)
                .collect(java.util.stream.Collectors.toList());
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error in getAllNonDeletedExecutions Execution: " + e.getMessage());
        }
    }


}
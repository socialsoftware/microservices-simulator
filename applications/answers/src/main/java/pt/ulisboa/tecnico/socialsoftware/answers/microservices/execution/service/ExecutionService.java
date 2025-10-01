package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.*;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.CourseExecutionCourse;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.CourseExecutionStudent;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.*;

import java.util.*;
import java.util.stream.Collectors;

import java.util.List;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import java.time.LocalDateTime;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersException;


@Service
@Transactional
public class ExecutionService {
    private static final Logger logger = LoggerFactory.getLogger(ExecutionService.class);

    @Autowired
    private ExecutionRepository executionRepository;

    @Autowired
    private ExecutionFactory executionFactory;

    public ExecutionService() {}

    // CRUD Operations
    public ExecutionDto createExecution(String name, String acronym, String academicTerm, LocalDateTime startDate, LocalDateTime endDate, Object course, Object students) {
        try {
            Execution execution = new Execution(name, acronym, academicTerm, startDate, endDate, course, students);
            execution = executionRepository.save(execution);
            return new ExecutionDto(execution);
        } catch (Exception e) {
            throw new AnswersException("Error creating execution: " + e.getMessage());
        }
    }

    public ExecutionDto getExecutionById(Integer id) {
        try {
            Execution execution = (Execution) executionRepository.findById(id)
                .orElseThrow(() -> new AnswersException("Execution not found with id: " + id));
            return new ExecutionDto(execution);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error retrieving execution: " + e.getMessage());
        }
    }

    public List<ExecutionDto> getAllExecutions() {
        try {
            return executionRepository.findAll().stream()
                .map(entity -> new ExecutionDto((Execution) entity))
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new AnswersException("Error retrieving all executions: " + e.getMessage());
        }
    }

    public ExecutionDto updateExecution(Integer id, ExecutionDto executionDto) {
        try {
            Execution execution = (Execution) executionRepository.findById(id)
                .orElseThrow(() -> new AnswersException("Execution not found with id: " + id));
            
                        if (executionDto.getName() != null) {
                execution.setName(executionDto.getName());
            }
            if (executionDto.getAcronym() != null) {
                execution.setAcronym(executionDto.getAcronym());
            }
            if (executionDto.getAcademicTerm() != null) {
                execution.setAcademicTerm(executionDto.getAcademicTerm());
            }
            if (executionDto.getStartDate() != null) {
                execution.setStartDate(executionDto.getStartDate());
            }
            if (executionDto.getEndDate() != null) {
                execution.setEndDate(executionDto.getEndDate());
            }
            if (executionDto.getCourse() != null) {
                execution.setCourse(executionDto.getCourse());
            }
            if (executionDto.getStudents() != null) {
                execution.setStudents(executionDto.getStudents());
            }
            
            execution = executionRepository.save(execution);
            return new ExecutionDto(execution);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error updating execution: " + e.getMessage());
        }
    }

    public void deleteExecution(Integer id) {
        try {
            if (!executionRepository.existsById(id)) {
                throw new AnswersException("Execution not found with id: " + id);
            }
            executionRepository.deleteById(id);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error deleting execution: " + e.getMessage());
        }
    }

    // Business Methods
    @Transactional
    public Object getActiveExecutions(Integer id, UnitOfWork unitOfWork) {
        try {
            Execution execution = executionRepository.findById(id)
                .orElseThrow(() -> new AnswersException("Execution not found with id: " + id));
            
            // Business logic for getActiveExecutions
            Object result = execution.getActiveExecutions();
            executionRepository.save(execution);
            return result;
        } catch (Exception e) {
            throw new AnswersException("Error in getActiveExecutions: " + e.getMessage());
        }
    }

    // Custom Workflow Methods
    @Transactional
    public void removeUser(Integer userAggregateId, Integer executionId, UnitOfWork unitOfWork) {
        try {
            // TODO: Implement workflow logic for removeUser
            throw new UnsupportedOperationException("Workflow removeUser not implemented");

        } catch (Exception e) {
            throw new AnswersException("Error in workflow removeUser: " + e.getMessage());
        }
    }

    @Transactional
    public void anonymizeStudent(Integer studentAggregateId, Integer executionId, UnitOfWork unitOfWork) {
        try {
            // TODO: Implement workflow logic for anonymizeStudent
            throw new UnsupportedOperationException("Workflow anonymizeStudent not implemented");

        } catch (Exception e) {
            throw new AnswersException("Error in workflow anonymizeStudent: " + e.getMessage());
        }
    }

    @Transactional
    public void updateStudentName(Integer studentAggregateId, String studentName, String studentUsername, Integer executionId, UnitOfWork unitOfWork) {
        try {
            // TODO: Implement workflow logic for updateStudentName
            throw new UnsupportedOperationException("Workflow updateStudentName not implemented");

        } catch (Exception e) {
            throw new AnswersException("Error in workflow updateStudentName: " + e.getMessage());
        }
    }

    @Transactional
    public void deleteExecution(Integer executionId, UnitOfWork unitOfWork) {
        try {
            // TODO: Implement workflow logic for deleteExecution
            throw new UnsupportedOperationException("Workflow deleteExecution not implemented");

        } catch (Exception e) {
            throw new AnswersException("Error in workflow deleteExecution: " + e.getMessage());
        }
    }

    @Transactional
    public void disenrollStudentFromExecution(Integer studentAggregateId, Integer executionId, UnitOfWork unitOfWork) {
        try {
            // TODO: Implement workflow logic for disenrollStudentFromExecution
            throw new UnsupportedOperationException("Workflow disenrollStudentFromExecution not implemented");

        } catch (Exception e) {
            throw new AnswersException("Error in workflow disenrollStudentFromExecution: " + e.getMessage());
        }
    }

    // Query methods disabled - repository methods not implemented

    // Event Processing Methods
    private void publishExecutionCreatedEvent(Execution execution) {
        try {
            // TODO: Implement event publishing for ExecutionCreated
            // eventPublisher.publishEvent(new ExecutionCreatedEvent(execution));
        } catch (Exception e) {
            // Log error but don't fail the transaction
            logger.error("Failed to publish ExecutionCreatedEvent", e);
        }
    }

    private void publishExecutionUpdatedEvent(Execution execution) {
        try {
            // TODO: Implement event publishing for ExecutionUpdated
            // eventPublisher.publishEvent(new ExecutionUpdatedEvent(execution));
        } catch (Exception e) {
            // Log error but don't fail the transaction
            logger.error("Failed to publish ExecutionUpdatedEvent", e);
        }
    }

    private void publishExecutionDeletedEvent(Long executionId) {
        try {
            // TODO: Implement event publishing for ExecutionDeleted
            // eventPublisher.publishEvent(new ExecutionDeletedEvent(executionId));
        } catch (Exception e) {
            // Log error but don't fail the transaction
            logger.error("Failed to publish ExecutionDeletedEvent", e);
        }
    }
}
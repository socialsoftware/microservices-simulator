package pt.ulisboa.tecnico.socialsoftware.answers.microservices.courseexecution.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.courseexecution.aggregate.*;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.courseexecution.aggregate.CourseExecutionCourse;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.courseexecution.aggregate.CourseExecutionStudent;
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
public class CourseExecutionService {
    private static final Logger logger = LoggerFactory.getLogger(CourseExecutionService.class);

    @Autowired
    private CourseExecutionRepository courseexecutionRepository;

    @Autowired
    private CourseExecutionFactory courseexecutionFactory;

    public CourseExecutionService() {}

    // CRUD Operations
    public CourseExecutionDto createCourseExecution(String name, String acronym, String academicTerm, LocalDateTime startDate, LocalDateTime endDate, Object course, Object students) {
        try {
            CourseExecution courseexecution = new CourseExecution(name, acronym, academicTerm, startDate, endDate, course, students);
            courseexecution = courseexecutionRepository.save(courseexecution);
            return new CourseExecutionDto(courseexecution);
        } catch (Exception e) {
            throw new AnswersException("Error creating courseexecution: " + e.getMessage());
        }
    }

    public CourseExecutionDto getCourseExecutionById(Integer id) {
        try {
            CourseExecution courseexecution = (CourseExecution) courseexecutionRepository.findById(id)
                .orElseThrow(() -> new AnswersException("CourseExecution not found with id: " + id));
            return new CourseExecutionDto(courseexecution);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error retrieving courseexecution: " + e.getMessage());
        }
    }

    public List<CourseExecutionDto> getAllCourseExecutions() {
        try {
            return courseexecutionRepository.findAll().stream()
                .map(entity -> new CourseExecutionDto((CourseExecution) entity))
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new AnswersException("Error retrieving all courseexecutions: " + e.getMessage());
        }
    }

    public CourseExecutionDto updateCourseExecution(Integer id, CourseExecutionDto courseexecutionDto) {
        try {
            CourseExecution courseexecution = (CourseExecution) courseexecutionRepository.findById(id)
                .orElseThrow(() -> new AnswersException("CourseExecution not found with id: " + id));
            
                        if (courseexecutionDto.getName() != null) {
                courseexecution.setName(courseexecutionDto.getName());
            }
            if (courseexecutionDto.getAcronym() != null) {
                courseexecution.setAcronym(courseexecutionDto.getAcronym());
            }
            if (courseexecutionDto.getAcademicTerm() != null) {
                courseexecution.setAcademicTerm(courseexecutionDto.getAcademicTerm());
            }
            if (courseexecutionDto.getStartDate() != null) {
                courseexecution.setStartDate(courseexecutionDto.getStartDate());
            }
            if (courseexecutionDto.getEndDate() != null) {
                courseexecution.setEndDate(courseexecutionDto.getEndDate());
            }
            if (courseexecutionDto.getCourse() != null) {
                courseexecution.setCourse(courseexecutionDto.getCourse());
            }
            if (courseexecutionDto.getStudents() != null) {
                courseexecution.setStudents(courseexecutionDto.getStudents());
            }
            
            courseexecution = courseexecutionRepository.save(courseexecution);
            return new CourseExecutionDto(courseexecution);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error updating courseexecution: " + e.getMessage());
        }
    }

    public void deleteCourseExecution(Integer id) {
        try {
            if (!courseexecutionRepository.existsById(id)) {
                throw new AnswersException("CourseExecution not found with id: " + id);
            }
            courseexecutionRepository.deleteById(id);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error deleting courseexecution: " + e.getMessage());
        }
    }

    // Business Methods
    @Transactional
    public Object getActiveCourseExecutions(Integer id, UnitOfWork unitOfWork) {
        try {
            CourseExecution courseexecution = courseexecutionRepository.findById(id)
                .orElseThrow(() -> new AnswersException("CourseExecution not found with id: " + id));
            
            // Business logic for getActiveCourseExecutions
            Object result = courseexecution.getActiveCourseExecutions();
            courseexecutionRepository.save(courseexecution);
            return result;
        } catch (Exception e) {
            throw new AnswersException("Error in getActiveCourseExecutions: " + e.getMessage());
        }
    }

    // Custom Workflow Methods
    @Transactional
    public void removeUser(Integer userAggregateId, Integer courseExecutionId, UnitOfWork unitOfWork) {
        try {
            // TODO: Implement workflow logic for removeUser
            throw new UnsupportedOperationException("Workflow removeUser not implemented");

        } catch (Exception e) {
            throw new AnswersException("Error in workflow removeUser: " + e.getMessage());
        }
    }

    @Transactional
    public void anonymizeStudent(Integer studentAggregateId, Integer courseExecutionId, UnitOfWork unitOfWork) {
        try {
            // TODO: Implement workflow logic for anonymizeStudent
            throw new UnsupportedOperationException("Workflow anonymizeStudent not implemented");

        } catch (Exception e) {
            throw new AnswersException("Error in workflow anonymizeStudent: " + e.getMessage());
        }
    }

    @Transactional
    public void updateStudentName(Integer studentAggregateId, String studentName, String studentUsername, Integer courseExecutionId, UnitOfWork unitOfWork) {
        try {
            // TODO: Implement workflow logic for updateStudentName
            throw new UnsupportedOperationException("Workflow updateStudentName not implemented");

        } catch (Exception e) {
            throw new AnswersException("Error in workflow updateStudentName: " + e.getMessage());
        }
    }

    @Transactional
    public void deleteCourseExecution(Integer courseExecutionId, UnitOfWork unitOfWork) {
        try {
            // TODO: Implement workflow logic for deleteCourseExecution
            throw new UnsupportedOperationException("Workflow deleteCourseExecution not implemented");

        } catch (Exception e) {
            throw new AnswersException("Error in workflow deleteCourseExecution: " + e.getMessage());
        }
    }

    @Transactional
    public void disenrollStudentFromCourseExecution(Integer studentAggregateId, Integer courseExecutionId, UnitOfWork unitOfWork) {
        try {
            // TODO: Implement workflow logic for disenrollStudentFromCourseExecution
            throw new UnsupportedOperationException("Workflow disenrollStudentFromCourseExecution not implemented");

        } catch (Exception e) {
            throw new AnswersException("Error in workflow disenrollStudentFromCourseExecution: " + e.getMessage());
        }
    }

    // Query methods disabled - repository methods not implemented

    // Event Processing Methods
    private void publishCourseExecutionCreatedEvent(CourseExecution courseexecution) {
        try {
            // TODO: Implement event publishing for CourseExecutionCreated
            // eventPublisher.publishEvent(new CourseExecutionCreatedEvent(courseexecution));
        } catch (Exception e) {
            // Log error but don't fail the transaction
            logger.error("Failed to publish CourseExecutionCreatedEvent", e);
        }
    }

    private void publishCourseExecutionUpdatedEvent(CourseExecution courseexecution) {
        try {
            // TODO: Implement event publishing for CourseExecutionUpdated
            // eventPublisher.publishEvent(new CourseExecutionUpdatedEvent(courseexecution));
        } catch (Exception e) {
            // Log error but don't fail the transaction
            logger.error("Failed to publish CourseExecutionUpdatedEvent", e);
        }
    }

    private void publishCourseExecutionDeletedEvent(Long courseexecutionId) {
        try {
            // TODO: Implement event publishing for CourseExecutionDeleted
            // eventPublisher.publishEvent(new CourseExecutionDeletedEvent(courseexecutionId));
        } catch (Exception e) {
            // Log error but don't fail the transaction
            logger.error("Failed to publish CourseExecutionDeletedEvent", e);
        }
    }
}
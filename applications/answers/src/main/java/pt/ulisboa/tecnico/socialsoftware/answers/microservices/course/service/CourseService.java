package pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.aggregate.*;

import pt.ulisboa.tecnico.socialsoftware.ms.exception.*;

import java.util.List;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.CourseType;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import java.time.LocalDateTime;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersException;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos.CreateCourseRequestDto;


@Service
@Transactional
public class CourseService {
    private static final Logger logger = LoggerFactory.getLogger(CourseService.class);

    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService<UnitOfWork> unitOfWorkService;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseFactory courseFactory;

    public CourseService() {}

    // CRUD Operations
    public CourseDto createCourse(CreateCourseRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            // Convert CreateRequestDto to regular DTO
            CourseDto courseDto = new CourseDto();
            courseDto.setName(createRequest.getName());
            courseDto.setType(createRequest.getType() != null ? createRequest.getType().name() : null);
            courseDto.setCreationDate(createRequest.getCreationDate());
            
            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Course course = courseFactory.createCourse(aggregateId, courseDto);
            unitOfWorkService.registerChanged(course, unitOfWork);
            return courseFactory.createCourseDto(course);
        } catch (Exception e) {
            throw new AnswersException("Error creating course: " + e.getMessage());
        }
    }

    public CourseDto getCourseById(Integer id) {
        try {
            Course course = (Course) courseRepository.findById(id)
                .orElseThrow(() -> new AnswersException("Course not found with id: " + id));
            return new CourseDto(course);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error retrieving course: " + e.getMessage());
        }
    }

    public List<CourseDto> getAllCourses() {
        try {
            return courseRepository.findAll().stream()
                .map(entity -> new CourseDto((Course) entity))
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new AnswersException("Error retrieving all courses: " + e.getMessage());
        }
    }

    public CourseDto updateCourse(CourseDto courseDto) {
        try {
            Integer id = courseDto.getAggregateId();
            Course course = (Course) courseRepository.findById(id)
                .orElseThrow(() -> new AnswersException("Course not found with id: " + id));
            
                        if (courseDto.getName() != null) {
                course.setName(courseDto.getName());
            }
            if (courseDto.getCreationDate() != null) {
                course.setCreationDate(courseDto.getCreationDate());
            }
            
            course = courseRepository.save(course);
            return new CourseDto(course);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error updating course: " + e.getMessage());
        }
    }

    public void deleteCourse(Integer id) {
        try {
            if (!courseRepository.existsById(id)) {
                throw new AnswersException("Course not found with id: " + id);
            }
            courseRepository.deleteById(id);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error deleting course: " + e.getMessage());
        }
    }

    // No business methods defined

    // No custom workflows defined

    // Query methods not implemented

    // Event Processing Methods
    private void publishCourseCreatedEvent(Course course) {
        try {
            // TODO: Implement event publishing for CourseCreated
            // eventPublisher.publishEvent(new CourseCreatedEvent(course));
        } catch (Exception e) {
            // Log error but don't fail the transaction
            logger.error("Failed to publish CourseCreatedEvent", e);
        }
    }

    private void publishCourseUpdatedEvent(Course course) {
        try {
            // TODO: Implement event publishing for CourseUpdated
            // eventPublisher.publishEvent(new CourseUpdatedEvent(course));
        } catch (Exception e) {
            // Log error but don't fail the transaction
            logger.error("Failed to publish CourseUpdatedEvent", e);
        }
    }

    private void publishCourseDeletedEvent(Long courseId) {
        try {
            // TODO: Implement event publishing for CourseDeleted
            // eventPublisher.publishEvent(new CourseDeletedEvent(courseId));
        } catch (Exception e) {
            // Log error but don't fail the transaction
            logger.error("Failed to publish CourseDeletedEvent", e);
        }
    }
}
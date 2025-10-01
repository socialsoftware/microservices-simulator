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
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import java.time.LocalDateTime;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersException;


@Service
@Transactional
public class CourseService {
    private static final Logger logger = LoggerFactory.getLogger(CourseService.class);

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseFactory courseFactory;

    public CourseService() {}

    // CRUD Operations
    public CourseDto createCourse(String name, String acronym, String courseType, LocalDateTime creationDate) {
        try {
            Course course = new Course(name, acronym, courseType, creationDate);
            course = courseRepository.save(course);
            return new CourseDto(course);
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

    public CourseDto updateCourse(Integer id, CourseDto courseDto) {
        try {
            Course course = (Course) courseRepository.findById(id)
                .orElseThrow(() -> new AnswersException("Course not found with id: " + id));
            
                        if (courseDto.getName() != null) {
                course.setName(courseDto.getName());
            }
            if (courseDto.getAcronym() != null) {
                course.setAcronym(courseDto.getAcronym());
            }
            if (courseDto.getCourseType() != null) {
                course.setCourseType(courseDto.getCourseType());
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

    // Business Methods
    @Transactional
    public List<Course> searchCoursesByName(Integer id, String name, UnitOfWork unitOfWork) {
        try {
            Course course = courseRepository.findById(id)
                .orElseThrow(() -> new AnswersException("Course not found with id: " + id));
            
            // Business logic for searchCoursesByName
            List<Course> result = course.searchCoursesByName();
            courseRepository.save(course);
            return result;
        } catch (Exception e) {
            throw new AnswersException("Error in searchCoursesByName: " + e.getMessage());
        }
    }

    @Transactional
    public List<Course> searchCoursesByAcronym(Integer id, String acronym, UnitOfWork unitOfWork) {
        try {
            Course course = courseRepository.findById(id)
                .orElseThrow(() -> new AnswersException("Course not found with id: " + id));
            
            // Business logic for searchCoursesByAcronym
            List<Course> result = course.searchCoursesByAcronym();
            courseRepository.save(course);
            return result;
        } catch (Exception e) {
            throw new AnswersException("Error in searchCoursesByAcronym: " + e.getMessage());
        }
    }

    @Transactional
    public Set<Object> getUniqueCourseTypes(Integer id, UnitOfWork unitOfWork) {
        try {
            Course course = courseRepository.findById(id)
                .orElseThrow(() -> new AnswersException("Course not found with id: " + id));
            
            // Business logic for getUniqueCourseTypes
            Set<Object> result = course.getUniqueCourseTypes();
            courseRepository.save(course);
            return result;
        } catch (Exception e) {
            throw new AnswersException("Error in getUniqueCourseTypes: " + e.getMessage());
        }
    }

    @Transactional
    public Set<Course> getCoursesAsSet(Integer id, UnitOfWork unitOfWork) {
        try {
            Course course = courseRepository.findById(id)
                .orElseThrow(() -> new AnswersException("Course not found with id: " + id));
            
            // Business logic for getCoursesAsSet
            Set<Course> result = course.getCoursesAsSet();
            courseRepository.save(course);
            return result;
        } catch (Exception e) {
            throw new AnswersException("Error in getCoursesAsSet: " + e.getMessage());
        }
    }

    // No custom workflows defined

    // Query methods disabled - repository methods not implemented

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
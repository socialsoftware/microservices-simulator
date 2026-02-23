package pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.CourseDto;
import java.time.LocalDateTime;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.events.publish.CourseDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.events.publish.CourseUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersException;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos.CreateCourseRequestDto;


@Service
@Transactional
public class CourseService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService<UnitOfWork> unitOfWorkService;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseFactory courseFactory;

    public CourseService() {}

    public CourseDto createCourse(CreateCourseRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            CourseDto courseDto = new CourseDto();
            courseDto.setName(createRequest.getName());
            courseDto.setType(createRequest.getType() != null ? createRequest.getType().name() : null);
            courseDto.setCreationDate(createRequest.getCreationDate());

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Course course = courseFactory.createCourse(aggregateId, courseDto);
            unitOfWorkService.registerChanged(course, unitOfWork);
            return courseFactory.createCourseDto(course);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error creating course: " + e.getMessage());
        }
    }

    public CourseDto getCourseById(Integer id, UnitOfWork unitOfWork) {
        try {
            Course course = (Course) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return courseFactory.createCourseDto(course);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error retrieving course: " + e.getMessage());
        }
    }

    public List<CourseDto> getAllCourses(UnitOfWork unitOfWork) {
        try {
            Set<Integer> aggregateIds = courseRepository.findAll().stream()
                .map(Course::getAggregateId)
                .collect(Collectors.toSet());

            return aggregateIds.stream()
                .map(id -> (Course) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .map(courseFactory::createCourseDto)
                .collect(Collectors.toList());
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error retrieving course: " + e.getMessage());
        }
    }

    public CourseDto updateCourse(CourseDto courseDto, UnitOfWork unitOfWork) {
        try {
            Integer id = courseDto.getAggregateId();
            Course oldCourse = (Course) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Course newCourse = courseFactory.createCourseFromExisting(oldCourse);
            if (courseDto.getName() != null) {
                newCourse.setName(courseDto.getName());
            }
            if (courseDto.getCreationDate() != null) {
                newCourse.setCreationDate(courseDto.getCreationDate());
            }

            unitOfWorkService.registerChanged(newCourse, unitOfWork);            CourseUpdatedEvent event = new CourseUpdatedEvent(newCourse.getAggregateId(), newCourse.getName(), newCourse.getCreationDate());
            event.setPublisherAggregateVersion(newCourse.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return courseFactory.createCourseDto(newCourse);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error updating course: " + e.getMessage());
        }
    }

    public void deleteCourse(Integer id, UnitOfWork unitOfWork) {
        try {
            Course oldCourse = (Course) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Course newCourse = courseFactory.createCourseFromExisting(oldCourse);
            newCourse.remove();
            unitOfWorkService.registerChanged(newCourse, unitOfWork);            unitOfWorkService.registerEvent(new CourseDeletedEvent(newCourse.getAggregateId()), unitOfWork);
        } catch (AnswersException e) {
            throw e;
        } catch (Exception e) {
            throw new AnswersException("Error deleting course: " + e.getMessage());
        }
    }








}
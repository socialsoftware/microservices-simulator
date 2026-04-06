package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.course.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.course.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.CourseTeacherDto;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.events.CourseDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.events.CourseUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.exception.CrossrefsException;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.course.coordination.webapi.requestDtos.CreateCourseRequestDto;


@Service
@Transactional
public class CourseService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService unitOfWorkService;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseFactory courseFactory;

    public CourseService() {}

    public CourseDto createCourse(CreateCourseRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            CourseDto courseDto = new CourseDto();
            courseDto.setTitle(createRequest.getTitle());
            courseDto.setDescription(createRequest.getDescription());
            courseDto.setMaxStudents(createRequest.getMaxStudents());
            if (createRequest.getTeacher() != null) {
                CourseTeacherDto teacherDto = new CourseTeacherDto();
                teacherDto.setAggregateId(createRequest.getTeacher().getAggregateId());
                teacherDto.setVersion(createRequest.getTeacher().getVersion());
                teacherDto.setState(createRequest.getTeacher().getState() != null ? createRequest.getTeacher().getState().name() : null);
                courseDto.setTeacher(teacherDto);
            }

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Course course = courseFactory.createCourse(aggregateId, courseDto);
            unitOfWorkService.registerChanged(course, unitOfWork);
            return courseFactory.createCourseDto(course);
        } catch (CrossrefsException e) {
            throw e;
        } catch (Exception e) {
            throw new CrossrefsException("Error creating course: " + e.getMessage());
        }
    }

    public CourseDto getCourseById(Integer id, UnitOfWork unitOfWork) {
        try {
            Course course = (Course) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return courseFactory.createCourseDto(course);
        } catch (CrossrefsException e) {
            throw e;
        } catch (Exception e) {
            throw new CrossrefsException("Error retrieving course: " + e.getMessage());
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
        } catch (CrossrefsException e) {
            throw e;
        } catch (Exception e) {
            throw new CrossrefsException("Error retrieving course: " + e.getMessage());
        }
    }

    public CourseDto updateCourse(CourseDto courseDto, UnitOfWork unitOfWork) {
        try {
            Integer id = courseDto.getAggregateId();
            Course oldCourse = (Course) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Course newCourse = courseFactory.createCourseFromExisting(oldCourse);
            if (courseDto.getTitle() != null) {
                newCourse.setTitle(courseDto.getTitle());
            }
            if (courseDto.getDescription() != null) {
                newCourse.setDescription(courseDto.getDescription());
            }
            if (courseDto.getMaxStudents() != null) {
                newCourse.setMaxStudents(courseDto.getMaxStudents());
            }

            unitOfWorkService.registerChanged(newCourse, unitOfWork);            CourseUpdatedEvent event = new CourseUpdatedEvent(newCourse.getAggregateId(), newCourse.getTitle(), newCourse.getDescription(), newCourse.getMaxStudents());
            event.setPublisherAggregateVersion(newCourse.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return courseFactory.createCourseDto(newCourse);
        } catch (CrossrefsException e) {
            throw e;
        } catch (Exception e) {
            throw new CrossrefsException("Error updating course: " + e.getMessage());
        }
    }

    public void deleteCourse(Integer id, UnitOfWork unitOfWork) {
        try {
            Course oldCourse = (Course) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Course newCourse = courseFactory.createCourseFromExisting(oldCourse);
            newCourse.remove();
            unitOfWorkService.registerChanged(newCourse, unitOfWork);            unitOfWorkService.registerEvent(new CourseDeletedEvent(newCourse.getAggregateId()), unitOfWork);
        } catch (CrossrefsException e) {
            throw e;
        } catch (Exception e) {
            throw new CrossrefsException("Error deleting course: " + e.getMessage());
        }
    }








}
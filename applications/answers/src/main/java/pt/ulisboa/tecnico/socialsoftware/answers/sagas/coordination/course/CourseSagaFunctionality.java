package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.course;

import java.time.LocalDateTime;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.service.CourseService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.aggregate.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos.SagaCourseDto;

@Component
public class CourseSagaFunctionality extends WorkflowFunctionality {
private final CourseService courseService;
private final SagaUnitOfWorkService unitOfWorkService;

public CourseSagaFunctionality(CourseService courseService, SagaUnitOfWorkService
unitOfWorkService) {
this.courseService = courseService;
this.unitOfWorkService = unitOfWorkService;
}

    public Object createCourse(String name, String acronym, String courseType, SagaUnitOfWork unitOfWork) {
        // TODO: Implement saga functionality for createCourse
        // This method should orchestrate the saga workflow
        return null;
    }

    public Object getCourseById(Integer courseId, SagaUnitOfWork unitOfWork) {
        // TODO: Implement saga functionality for getCourseById
        // This method should orchestrate the saga workflow
        return null;
    }

    public List<Course> getAllCourses(SagaUnitOfWork unitOfWork) {
        // TODO: Implement saga functionality for getAllCourses
        // This method should orchestrate the saga workflow
        return null;
    }

    public Object updateCourse(Integer courseId, String name, String acronym, String courseType, SagaUnitOfWork unitOfWork) {
        // TODO: Implement saga functionality for updateCourse
        // This method should orchestrate the saga workflow
        return null;
    }

    public Object deleteCourse(Integer courseId, SagaUnitOfWork unitOfWork) {
        // TODO: Implement saga functionality for deleteCourse
        // This method should orchestrate the saga workflow
        return null;
    }
}
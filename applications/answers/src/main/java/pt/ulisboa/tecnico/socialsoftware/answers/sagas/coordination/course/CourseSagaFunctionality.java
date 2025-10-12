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


}
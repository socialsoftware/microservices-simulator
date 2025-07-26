package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.execution;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SagasCommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.courseExecution.GetAllCourseExecutionsCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.service.CourseExecutionService;

import java.util.List;

public class GetCourseExecutionsFunctionalitySagas extends WorkflowFunctionality {
    private List<CourseExecutionDto> courseExecutions;
    private final CourseExecutionService courseExecutionService;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final SagasCommandGateway sagasCommandGateway;

    public GetCourseExecutionsFunctionalitySagas(CourseExecutionService courseExecutionService, SagaUnitOfWorkService unitOfWorkService, 
                                    SagaUnitOfWork unitOfWork, SagasCommandGateway sagasCommandGateway) {
        this.courseExecutionService = courseExecutionService;
        this.unitOfWorkService = unitOfWorkService;
        this.sagasCommandGateway = sagasCommandGateway;
        this.buildWorkflow(unitOfWork);
    }

    public void buildWorkflow(SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getCourseExecutionsStep = new SagaSyncStep("getCourseExecutionsStep", () -> {
            // List<CourseExecutionDto> courseExecutions = courseExecutionService.getAllCourseExecutions(unitOfWork);
            GetAllCourseExecutionsCommand getAllCourseExecutionsCommand = new GetAllCourseExecutionsCommand(unitOfWork, "courseExecutionService");
            List<CourseExecutionDto> courseExecutions = (List<CourseExecutionDto>) sagasCommandGateway.send(getAllCourseExecutionsCommand);
            this.setCourseExecutions(courseExecutions);
        });
    
        workflow.addStep(getCourseExecutionsStep);
    }
    

    public List<CourseExecutionDto> getCourseExecutions() {
        return courseExecutions;
    }

    public void setCourseExecutions(List<CourseExecutionDto> courseExecutions) {
        this.courseExecutions = courseExecutions;
    }
}
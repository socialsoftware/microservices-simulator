package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.execution;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SagasCommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.courseExecution.GetCourseExecutionsByUserIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.service.CourseExecutionService;

import java.util.Set;

public class GetCourseExecutionsByUserFunctionalitySagas extends WorkflowFunctionality {
    private Set<CourseExecutionDto> courseExecutions;
    private final CourseExecutionService courseExecutionService;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final SagasCommandGateway sagasCommandGateway;

    public GetCourseExecutionsByUserFunctionalitySagas(CourseExecutionService courseExecutionService,
            SagaUnitOfWorkService unitOfWorkService,
            Integer userAggregateId, SagaUnitOfWork unitOfWork, SagasCommandGateway sagasCommandGateway) {
        this.courseExecutionService = courseExecutionService;
        this.unitOfWorkService = unitOfWorkService;
        this.sagasCommandGateway = sagasCommandGateway;
        this.buildWorkflow(userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getCourseExecutionsByUserStep = new SagaSyncStep("getCourseExecutionsByUserStep", () -> {
            // Set<CourseExecutionDto> courseExecutions =
            // courseExecutionService.getCourseExecutionsByUserId(userAggregateId,
            // unitOfWork);
            GetCourseExecutionsByUserIdCommand getCourseExecutionsByUserIdCommand = new GetCourseExecutionsByUserIdCommand(
                    unitOfWork, ServiceMapping.COURSE_EXECUTION.getServiceName(), userAggregateId);
            Set<CourseExecutionDto> courseExecutions = (Set<CourseExecutionDto>) sagasCommandGateway
                    .send(getCourseExecutionsByUserIdCommand);
            this.setCourseExecutions(courseExecutions);
        });

        workflow.addStep(getCourseExecutionsByUserStep);
    }

    public Set<CourseExecutionDto> getCourseExecutions() {
        return courseExecutions;
    }

    public void setCourseExecutions(Set<CourseExecutionDto> courseExecutions) {
        this.courseExecutions = courseExecutions;
    }
}
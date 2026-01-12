package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.causal;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Step;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.courseExecution.GetAllCourseExecutionsCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;

import java.util.List;

public class GetCourseExecutionsFunctionalityTCC extends WorkflowFunctionality {
    private List<CourseExecutionDto> courseExecutions;
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public GetCourseExecutionsFunctionalityTCC(CausalUnitOfWorkService unitOfWorkService,
                                               CausalUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(unitOfWork);
    }

    public void buildWorkflow(CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        Step step = new Step(() -> {
            GetAllCourseExecutionsCommand getAllCourseExecutionsCommand = new GetAllCourseExecutionsCommand(unitOfWork, ServiceMapping.COURSE_EXECUTION.getServiceName());
            this.courseExecutions = (List<CourseExecutionDto>) commandGateway.send(getAllCourseExecutionsCommand);
        });

        workflow.addStep(step);
    }

    public List<CourseExecutionDto> getCourseExecutions() {
        return courseExecutions;
    }

    public void setCourseExecutions(List<CourseExecutionDto> courseExecutions) {
        this.courseExecutions = courseExecutions;
    }
}
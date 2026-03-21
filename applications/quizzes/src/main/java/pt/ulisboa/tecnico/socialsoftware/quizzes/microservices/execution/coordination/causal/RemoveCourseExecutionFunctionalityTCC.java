package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.causal;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Step;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.course.GetCourseByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.course.UpdateCourseExecutionCountCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.execution.GetCourseExecutionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.execution.RemoveCourseExecutionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesException;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.causal.CausalExecution;

public class RemoveCourseExecutionFunctionalityTCC extends WorkflowFunctionality {
    private CausalExecution courseExecution;
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public RemoveCourseExecutionFunctionalityTCC(CausalUnitOfWorkService unitOfWorkService,
                                                 Integer executionAggregateId, CausalUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(executionAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer executionAggregateId, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        Step step = new Step(() -> {
            GetCourseExecutionByIdCommand getExecutionCmd = new GetCourseExecutionByIdCommand(unitOfWork, ServiceMapping.EXECUTION.getServiceName(), executionAggregateId);
            CourseExecutionDto executionDto = (CourseExecutionDto) commandGateway.send(getExecutionCmd);
            Integer courseAggregateId = executionDto.getCourseAggregateId();

            GetCourseByIdCommand getCourseCmd = new GetCourseByIdCommand(unitOfWork, ServiceMapping.COURSE.getServiceName(), courseAggregateId);
            CourseDto courseDto = (CourseDto) commandGateway.send(getCourseCmd);

            // CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT
            if (courseDto.getCourseQuestionCount() > 0 && courseDto.getCourseExecutionCount() == 1) {
                throw new QuizzesException(QuizzesErrorMessage.CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT, executionAggregateId);
            }

            RemoveCourseExecutionCommand removeCourseExecutionCommand = new RemoveCourseExecutionCommand(unitOfWork, ServiceMapping.EXECUTION.getServiceName(), executionAggregateId);
            commandGateway.send(removeCourseExecutionCommand);

            // CANNOT_DELETE_LAST_EXECUTION_WITH_CONTENT
            UpdateCourseExecutionCountCommand updateCmd = new UpdateCourseExecutionCountCommand(unitOfWork, ServiceMapping.COURSE.getServiceName(), courseAggregateId, false);
            commandGateway.send(updateCmd);
        });

        workflow.addStep(step);
    }

    public CausalExecution getCourseExecution() {
        return courseExecution;
    }

    public void setCourseExecution(CausalExecution courseExecution) {
        this.courseExecution = courseExecution;
    }
}
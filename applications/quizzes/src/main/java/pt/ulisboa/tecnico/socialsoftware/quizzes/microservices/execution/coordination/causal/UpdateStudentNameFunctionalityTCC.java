package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.causal;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.Step;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.commands.execution.UpdateExecutionStudentNameCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.Execution;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.causal.CausalUser;

public class UpdateStudentNameFunctionalityTCC extends WorkflowFunctionality {
    private CausalUser student;
    private Execution oldExecution;
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public UpdateStudentNameFunctionalityTCC(CausalUnitOfWorkService unitOfWorkService,
                                             Integer executionAggregateId, Integer userAggregateId, UserDto userDto, CausalUnitOfWork unitOfWork,
                                             CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(executionAggregateId, userAggregateId, userDto, unitOfWork);
    }

    public void buildWorkflow(Integer executionAggregateId, Integer userAggregateId, UserDto userDto,
            CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        Step step = new Step(() -> {
            UpdateExecutionStudentNameCommand updateExecutionStudentNameCommand = new UpdateExecutionStudentNameCommand(unitOfWork, ServiceMapping.EXECUTION.getServiceName(), executionAggregateId, userAggregateId, userDto.getName());
            commandGateway.send(updateExecutionStudentNameCommand);
        });

        workflow.addStep(step);
    }

    public CausalUser getStudent() {
        return student;
    }

    public void setStudent(CausalUser student) {
        this.student = student;
    }

    public Execution getOldCourseExecution() {
        return oldExecution;
    }

    public void setOldCourseExecution(Execution oldExecution) {
        this.oldExecution = oldExecution;
    }
}

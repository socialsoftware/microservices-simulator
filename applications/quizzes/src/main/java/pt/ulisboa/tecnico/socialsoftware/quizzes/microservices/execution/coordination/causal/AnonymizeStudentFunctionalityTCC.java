package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.causal;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.courseExecution.AnonymizeStudentCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecution;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.service.CourseExecutionService;

public class AnonymizeStudentFunctionalityTCC extends WorkflowFunctionality {
    private CourseExecution oldCourseExecution;
    private final CourseExecutionService courseExecutionService;
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public AnonymizeStudentFunctionalityTCC(CourseExecutionService courseExecutionService,
            CausalUnitOfWorkService unitOfWorkService, CourseExecutionFactory courseExecutionFactory,
            Integer executionAggregateId, Integer userAggregateId, CausalUnitOfWork unitOfWork,
            CommandGateway commandGateway) {
        this.courseExecutionService = courseExecutionService;
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(executionAggregateId, userAggregateId, courseExecutionFactory, unitOfWork);
    }

    public void buildWorkflow(Integer executionAggregateId, Integer userAggregateId,
            CourseExecutionFactory courseExecutionFactory, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
            // courseExecutionService.anonymizeStudent(executionAggregateId,
            // userAggregateId, unitOfWork);
            AnonymizeStudentCommand anonymizeStudentCommand = new AnonymizeStudentCommand(unitOfWork,
                    ServiceMapping.COURSE_EXECUTION.getServiceName(), executionAggregateId, userAggregateId);
            commandGateway.send(anonymizeStudentCommand);
        });

        workflow.addStep(step);
    }

    public CourseExecution getOldCourseExecution() {
        return oldCourseExecution;
    }

    public void setOldCourseExecution(CourseExecution oldCourseExecution) {
        this.oldCourseExecution = oldCourseExecution;
    }
}
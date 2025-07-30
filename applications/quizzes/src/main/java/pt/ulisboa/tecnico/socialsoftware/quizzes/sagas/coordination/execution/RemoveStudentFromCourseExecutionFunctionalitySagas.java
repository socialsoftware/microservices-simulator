package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.execution;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.courseExecution.GetCourseExecutionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.courseExecution.RemoveStudentFromCourseExecutionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.dtos.SagaCourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.states.CourseExecutionSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class RemoveStudentFromCourseExecutionFunctionalitySagas extends WorkflowFunctionality {
    
    private CourseExecutionDto oldCourseExecution;
    private final CourseExecutionService courseExecutionService;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public RemoveStudentFromCourseExecutionFunctionalitySagas(CourseExecutionService courseExecutionService, SagaUnitOfWorkService unitOfWorkService, CourseExecutionFactory courseExecutionFactory, 
                                                Integer courseExecutionAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.courseExecutionService = courseExecutionService;
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(courseExecutionAggregateId, userAggregateId, courseExecutionFactory, unitOfWork);
    }

    public void buildWorkflow(Integer courseExecutionAggregateId, Integer userAggregateId, CourseExecutionFactory courseExecutionFactory, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getOldCourseExecutionStep = new SagaSyncStep("getOldCourseExecutionStep", () -> {
//            CourseExecutionDto oldCourseExecution = (CourseExecutionDto) courseExecutionService.getCourseExecutionById(courseExecutionAggregateId, unitOfWork);
//            unitOfWorkService.registerSagaState(courseExecutionAggregateId, CourseExecutionSagaState.READ_COURSE, unitOfWork);
            GetCourseExecutionByIdCommand getCourseExecutionByIdCommand = new GetCourseExecutionByIdCommand(unitOfWork, ServiceMapping.COURSE_EXECUTION.getServiceName(), courseExecutionAggregateId);
            getCourseExecutionByIdCommand.setSemanticLock(CourseExecutionSagaState.READ_COURSE);
            CourseExecutionDto courseExecutionDto = (CourseExecutionDto) commandGateway.send(getCourseExecutionByIdCommand);
            this.setOldCourseExecution(oldCourseExecution);
        });
    
        getOldCourseExecutionStep.registerCompensation(() -> {
//            unitOfWorkService.registerSagaState(courseExecutionAggregateId, GenericSagaState.NOT_IN_SAGA, unitOfWork);
            Command command = new Command(unitOfWork, ServiceMapping.COURSE_EXECUTION.getServiceName(), courseExecutionAggregateId);
            command.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
            commandGateway.send(command);
        }, unitOfWork);
    
        SagaSyncStep removeStudentStep = new SagaSyncStep("removeStudentStep", () -> {
//            courseExecutionService.removeStudentFromCourseExecution(courseExecutionAggregateId, userAggregateId, unitOfWork);
            RemoveStudentFromCourseExecutionCommand removeStudentFromCourseExecutionCommand = new RemoveStudentFromCourseExecutionCommand(unitOfWork, ServiceMapping.COURSE_EXECUTION.getServiceName(), courseExecutionAggregateId, userAggregateId);
            commandGateway.send(removeStudentFromCourseExecutionCommand);
        }, new ArrayList<>(Arrays.asList(getOldCourseExecutionStep)));
    
        workflow.addStep(getOldCourseExecutionStep);
        workflow.addStep(removeStudentStep);
    }
    

    public CourseExecutionDto getOldCourseExecution() {
        return oldCourseExecution;
    }

    public void setOldCourseExecution(CourseExecutionDto oldCourseExecution) {
        this.oldCourseExecution = oldCourseExecution;
    }
}
package pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.command.post.*;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.shared.dtos.PostDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.aggregate.sagas.states.PostSagaState;

public class UpdatePostFunctionalitySagas extends WorkflowFunctionality {
    private PostDto updatedPostDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public UpdatePostFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, PostDto postDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(postDto, unitOfWork);
    }

    public void buildWorkflow(PostDto postDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep updatePostStep = new SagaStep("updatePostStep", () -> {
            unitOfWorkService.verifySagaState(postDto.getAggregateId(), new java.util.ArrayList<SagaState>(java.util.Arrays.asList(PostSagaState.READ_POST, PostSagaState.UPDATE_POST, PostSagaState.DELETE_POST)));
            unitOfWorkService.registerSagaState(postDto.getAggregateId(), PostSagaState.UPDATE_POST, unitOfWork);
            UpdatePostCommand cmd = new UpdatePostCommand(unitOfWork, ServiceMapping.POST.getServiceName(), postDto);
            PostDto updatedPostDto = (PostDto) commandGateway.send(cmd);
            setUpdatedPostDto(updatedPostDto);
        });

        workflow.addStep(updatePostStep);
    }
    public PostDto getUpdatedPostDto() {
        return updatedPostDto;
    }

    public void setUpdatedPostDto(PostDto updatedPostDto) {
        this.updatedPostDto = updatedPostDto;
    }
}

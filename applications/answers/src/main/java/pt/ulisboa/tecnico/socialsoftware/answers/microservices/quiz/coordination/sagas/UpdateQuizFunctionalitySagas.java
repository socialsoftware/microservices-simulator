package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.answers.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.answers.command.quiz.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateQuizFunctionalitySagas extends WorkflowFunctionality {
    private QuizDto updatedQuizDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public UpdateQuizFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, QuizDto quizDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(quizDto, unitOfWork);
    }

    public void buildWorkflow(QuizDto quizDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep updateQuizStep = new SagaStep("updateQuizStep", () -> {
            UpdateQuizCommand cmd = new UpdateQuizCommand(unitOfWork, ServiceMapping.QUIZ.getServiceName(), quizDto);
            QuizDto updatedQuizDto = (QuizDto) commandGateway.send(cmd);
            setUpdatedQuizDto(updatedQuizDto);
        });

        workflow.addStep(updateQuizStep);
    }
    public QuizDto getUpdatedQuizDto() {
        return updatedQuizDto;
    }

    public void setUpdatedQuizDto(QuizDto updatedQuizDto) {
        this.updatedQuizDto = updatedQuizDto;
    }
}

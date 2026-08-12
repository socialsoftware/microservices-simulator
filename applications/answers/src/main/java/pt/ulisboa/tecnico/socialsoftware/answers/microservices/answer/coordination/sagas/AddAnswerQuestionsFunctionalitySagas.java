package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.answers.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.answers.command.answer.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerQuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class AddAnswerQuestionsFunctionalitySagas extends WorkflowFunctionality {
    private List<AnswerQuestionDto> addedQuestionDtos;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public AddAnswerQuestionsFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer answerId, List<AnswerQuestionDto> questionDtos, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(answerId, questionDtos, unitOfWork);
    }

    public void buildWorkflow(Integer answerId, List<AnswerQuestionDto> questionDtos, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep addQuestionsStep = new SagaStep("addQuestionsStep", () -> {
            AddAnswerQuestionsCommand cmd = new AddAnswerQuestionsCommand(unitOfWork, ServiceMapping.ANSWER.getServiceName(), answerId, questionDtos);
            List<AnswerQuestionDto> addedQuestionDtos = (List<AnswerQuestionDto>) commandGateway.send(cmd);
            setAddedQuestionDtos(addedQuestionDtos);
        });

        workflow.addStep(addQuestionsStep);
    }
    public List<AnswerQuestionDto> getAddedQuestionDtos() {
        return addedQuestionDtos;
    }

    public void setAddedQuestionDtos(List<AnswerQuestionDto> addedQuestionDtos) {
        this.addedQuestionDtos = addedQuestionDtos;
    }
}

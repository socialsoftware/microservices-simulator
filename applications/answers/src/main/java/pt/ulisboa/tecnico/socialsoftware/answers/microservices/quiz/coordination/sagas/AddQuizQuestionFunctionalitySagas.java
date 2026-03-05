package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.answers.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.answers.command.quiz.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizQuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class AddQuizQuestionFunctionalitySagas extends WorkflowFunctionality {
    private QuizQuestionDto addedQuestionDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public AddQuizQuestionFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer quizId, Integer questionAggregateId, QuizQuestionDto questionDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(quizId, questionAggregateId, questionDto, unitOfWork);
    }

    public void buildWorkflow(Integer quizId, Integer questionAggregateId, QuizQuestionDto questionDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep addQuestionStep = new SagaStep("addQuestionStep", () -> {
            AddQuizQuestionCommand cmd = new AddQuizQuestionCommand(unitOfWork, ServiceMapping.QUIZ.getServiceName(), quizId, questionAggregateId, questionDto);
            QuizQuestionDto addedQuestionDto = (QuizQuestionDto) commandGateway.send(cmd);
            setAddedQuestionDto(addedQuestionDto);
        });

        workflow.addStep(addQuestionStep);
    }
    public QuizQuestionDto getAddedQuestionDto() {
        return addedQuestionDto;
    }

    public void setAddedQuestionDto(QuizQuestionDto addedQuestionDto) {
        this.addedQuestionDto = addedQuestionDto;
    }
}

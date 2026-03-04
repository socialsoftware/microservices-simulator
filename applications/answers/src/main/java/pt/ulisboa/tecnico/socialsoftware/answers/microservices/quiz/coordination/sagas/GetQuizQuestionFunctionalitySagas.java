package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.answers.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.answers.command.quiz.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizQuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class GetQuizQuestionFunctionalitySagas extends WorkflowFunctionality {
    private QuizQuestionDto questionDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public GetQuizQuestionFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer quizId, Integer questionAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(quizId, questionAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer quizId, Integer questionAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getQuestionStep = new SagaStep("getQuestionStep", () -> {
            GetQuizQuestionCommand cmd = new GetQuizQuestionCommand(unitOfWork, ServiceMapping.QUIZ.getServiceName(), quizId, questionAggregateId);
            QuizQuestionDto questionDto = (QuizQuestionDto) commandGateway.send(cmd);
            setQuestionDto(questionDto);
        });

        workflow.addStep(getQuestionStep);
    }
    public QuizQuestionDto getQuestionDto() {
        return questionDto;
    }

    public void setQuestionDto(QuizQuestionDto questionDto) {
        this.questionDto = questionDto;
    }
}

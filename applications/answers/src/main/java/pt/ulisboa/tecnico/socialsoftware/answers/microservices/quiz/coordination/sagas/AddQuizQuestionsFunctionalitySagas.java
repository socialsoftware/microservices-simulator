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
import java.util.List;

public class AddQuizQuestionsFunctionalitySagas extends WorkflowFunctionality {
    private List<QuizQuestionDto> addedQuestionDtos;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public AddQuizQuestionsFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer quizId, List<QuizQuestionDto> questionDtos, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(quizId, questionDtos, unitOfWork);
    }

    public void buildWorkflow(Integer quizId, List<QuizQuestionDto> questionDtos, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep addQuestionsStep = new SagaStep("addQuestionsStep", () -> {
            AddQuizQuestionsCommand cmd = new AddQuizQuestionsCommand(unitOfWork, ServiceMapping.QUIZ.getServiceName(), quizId, questionDtos);
            List<QuizQuestionDto> addedQuestionDtos = (List<QuizQuestionDto>) commandGateway.send(cmd);
            setAddedQuestionDtos(addedQuestionDtos);
        });

        workflow.addStep(addQuestionsStep);
    }
    public List<QuizQuestionDto> getAddedQuestionDtos() {
        return addedQuestionDtos;
    }

    public void setAddedQuestionDtos(List<QuizQuestionDto> addedQuestionDtos) {
        this.addedQuestionDtos = addedQuestionDtos;
    }
}

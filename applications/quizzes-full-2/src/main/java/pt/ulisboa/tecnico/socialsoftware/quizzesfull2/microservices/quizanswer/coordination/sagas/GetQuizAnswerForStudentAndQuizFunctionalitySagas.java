package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.quizanswer.GetQuizAnswerForStudentAndQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate.QuizAnswerDto;

// One step: both filter criteria are cached on the QuizAnswer itself, so no upstream fetch is needed
// to resolve them (docs/concepts/sagas.md § Two-step read saga variant).
public class GetQuizAnswerForStudentAndQuizFunctionalitySagas extends WorkflowFunctionality {
    private QuizAnswerDto quizAnswerDto;

    public GetQuizAnswerForStudentAndQuizFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                                            Integer userAggregateId, Integer quizAggregateId,
                                                            SagaUnitOfWork unitOfWork,
                                                            CommandGateway commandGateway) {
        buildWorkflow(unitOfWorkService, userAggregateId, quizAggregateId, unitOfWork, commandGateway);
    }

    public void buildWorkflow(SagaUnitOfWorkService unitOfWorkService,
                              Integer userAggregateId, Integer quizAggregateId,
                              SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getQuizAnswerForStudentAndQuizStep = new SagaStep("getQuizAnswerForStudentAndQuizStep", () -> {
            GetQuizAnswerForStudentAndQuizCommand cmd = new GetQuizAnswerForStudentAndQuizCommand(
                    unitOfWork, ServiceMapping.QUIZ_ANSWER.getServiceName(), userAggregateId, quizAggregateId);
            this.quizAnswerDto = (QuizAnswerDto) commandGateway.send(cmd);
        });

        this.workflow.addStep(getQuizAnswerForStudentAndQuizStep);
    }

    public QuizAnswerDto getQuizAnswerDto() {
        return quizAnswerDto;
    }
}

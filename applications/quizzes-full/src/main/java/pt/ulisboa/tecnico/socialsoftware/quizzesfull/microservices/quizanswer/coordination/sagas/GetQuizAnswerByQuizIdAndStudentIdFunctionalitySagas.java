package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.quizanswer.GetQuizAnswerByQuizIdAndStudentIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.aggregate.QuizAnswerDto;

public class GetQuizAnswerByQuizIdAndStudentIdFunctionalitySagas extends WorkflowFunctionality {
    private QuizAnswerDto quizAnswerDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public GetQuizAnswerByQuizIdAndStudentIdFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                                                Integer quizAggregateId,
                                                                Integer userAggregateId,
                                                                SagaUnitOfWork unitOfWork,
                                                                CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(quizAggregateId, userAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer quizAggregateId, Integer userAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getQuizAnswerStep = new SagaStep("getQuizAnswerStep", () -> {
            GetQuizAnswerByQuizIdAndStudentIdCommand cmd = new GetQuizAnswerByQuizIdAndStudentIdCommand(
                    unitOfWork, ServiceMapping.ANSWER.getServiceName(), quizAggregateId, userAggregateId);
            this.quizAnswerDto = (QuizAnswerDto) commandGateway.send(cmd);
        });

        this.workflow.addStep(getQuizAnswerStep);
    }

    public QuizAnswerDto getQuizAnswerDto() {
        return quizAnswerDto;
    }
}

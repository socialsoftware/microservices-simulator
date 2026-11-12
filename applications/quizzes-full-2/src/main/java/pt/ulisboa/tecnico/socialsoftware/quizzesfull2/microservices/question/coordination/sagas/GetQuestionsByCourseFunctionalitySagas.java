package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.question.GetQuestionsByCourseCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.QuestionDto;

import java.util.List;

public class GetQuestionsByCourseFunctionalitySagas extends WorkflowFunctionality {
    private List<QuestionDto> questions;

    public GetQuestionsByCourseFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                                  Integer courseAggregateId,
                                                  SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        buildWorkflow(unitOfWorkService, courseAggregateId, unitOfWork, commandGateway);
    }

    @SuppressWarnings("unchecked")
    public void buildWorkflow(SagaUnitOfWorkService unitOfWorkService,
                              Integer courseAggregateId,
                              SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getQuestionsByCourseStep = new SagaStep("getQuestionsByCourseStep", () -> {
            GetQuestionsByCourseCommand cmd = new GetQuestionsByCourseCommand(
                    unitOfWork, ServiceMapping.QUESTION.getServiceName(), courseAggregateId);
            this.questions = (List<QuestionDto>) commandGateway.send(cmd);
        });

        this.workflow.addStep(getQuestionsByCourseStep);
    }

    public List<QuestionDto> getQuestions() {
        return questions;
    }
}

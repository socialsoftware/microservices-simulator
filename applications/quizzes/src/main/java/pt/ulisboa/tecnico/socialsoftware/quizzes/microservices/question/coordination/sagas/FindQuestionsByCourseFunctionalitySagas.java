package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.question.FindQuestionsByCourseAggregateIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;

import java.util.List;

public class FindQuestionsByCourseFunctionalitySagas extends WorkflowFunctionality {
    private List<QuestionDto> questions;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public FindQuestionsByCourseFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                                   Integer courseAggregateId, SagaUnitOfWork unitOfWork, CommandGateway CommandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = CommandGateway;
        this.buildWorkflow(courseAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer courseAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep findQuestionsStep = new SagaSyncStep("findQuestionsStep", () -> {
            FindQuestionsByCourseAggregateIdCommand findQuestionsByCourseAggregateIdCommand = new FindQuestionsByCourseAggregateIdCommand(unitOfWork, ServiceMapping.QUESTION.getServiceName(), courseAggregateId);
            List<QuestionDto> questions = (List<QuestionDto>) commandGateway.send(findQuestionsByCourseAggregateIdCommand);
            this.setQuestions(questions);
        });

        workflow.addStep(findQuestionsStep);
    }

    public List<QuestionDto> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionDto> questions) {
        this.questions = questions;
    }
}

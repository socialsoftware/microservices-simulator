package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.question;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.question.FindQuestionsByCourseAggregateIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.service.QuestionService;

import java.util.List;

public class FindQuestionsByCourseFunctionalitySagas extends WorkflowFunctionality {
    private List<QuestionDto> questions;
    private final QuestionService questionService;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway CommandGateway;

    public FindQuestionsByCourseFunctionalitySagas(QuestionService questionService,
            SagaUnitOfWorkService unitOfWorkService,
            Integer courseAggregateId, SagaUnitOfWork unitOfWork, CommandGateway CommandGateway) {
        this.questionService = questionService;
        this.unitOfWorkService = unitOfWorkService;
        this.CommandGateway = CommandGateway;
        this.buildWorkflow(courseAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer courseAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep findQuestionsStep = new SagaSyncStep("findQuestionsStep", () -> {
            // List<QuestionDto> questions =
            // questionService.findQuestionsByCourseAggregateId(courseAggregateId,
            // unitOfWork);
            FindQuestionsByCourseAggregateIdCommand findQuestionsByCourseAggregateIdCommand = new FindQuestionsByCourseAggregateIdCommand(
                    unitOfWork, ServiceMapping.QUESTION.getServiceName(), courseAggregateId);
            List<QuestionDto> questions = (List<QuestionDto>) CommandGateway
                    .send(findQuestionsByCourseAggregateIdCommand);
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

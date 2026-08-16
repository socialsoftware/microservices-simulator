package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.question.GetQuestionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.quiz.GetQuizByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.quiz.UpdateQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizQuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.sagas.states.QuizSagaState;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UpdateQuizFunctionalitySagas extends WorkflowFunctionality {
    private QuizDto quizDto;
    private final List<QuizQuestionDto> questions = new ArrayList<>();

    public UpdateQuizFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer quizAggregateId,
                                        String title, LocalDateTime availableDate, LocalDateTime conclusionDate,
                                        LocalDateTime resultsDate, List<Integer> questionAggregateIds,
                                        SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        buildWorkflow(unitOfWorkService, quizAggregateId, title, availableDate, conclusionDate, resultsDate,
                questionAggregateIds, unitOfWork, commandGateway);
    }

    public void buildWorkflow(SagaUnitOfWorkService unitOfWorkService, Integer quizAggregateId, String title,
                              LocalDateTime availableDate, LocalDateTime conclusionDate,
                              LocalDateTime resultsDate, List<Integer> questionAggregateIds,
                              SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        // P4a prerequisite: re-seeds each QuizQuestion snapshot with the question's cached version,
        // title and content.
        SagaStep getQuestionsStep = new SagaStep("getQuestionsStep", () -> {
            for (Integer questionAggregateId : questionAggregateIds) {
                GetQuestionByIdCommand cmd = new GetQuestionByIdCommand(
                        unitOfWork, ServiceMapping.QUESTION.getServiceName(), questionAggregateId);
                QuestionDto questionDto = (QuestionDto) commandGateway.send(cmd);
                this.questions.add(new QuizQuestionDto(questionDto.getAggregateId(), questionDto.getVersion(),
                        questionDto.getTitle(), questionDto.getContent()));
            }
        });

        SagaStep getQuizStep = new SagaStep("getQuizStep", () -> {
            GetQuizByIdCommand readCmd = new GetQuizByIdCommand(
                    unitOfWork, ServiceMapping.QUIZ.getServiceName(), quizAggregateId);
            SagaCommand sagaCommand = new SagaCommand(readCmd);
            sagaCommand.setSemanticLock(QuizSagaState.IN_UPDATE_QUIZ);
            this.quizDto = (QuizDto) commandGateway.send(sagaCommand);
        });

        SagaStep updateQuizStep = new SagaStep("updateQuizStep", () -> {
            UpdateQuizCommand cmd = new UpdateQuizCommand(
                    unitOfWork, ServiceMapping.QUIZ.getServiceName(), quizAggregateId, title, availableDate,
                    conclusionDate, resultsDate, this.questions);
            commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getQuestionsStep, getQuizStep)));

        this.workflow.addStep(getQuestionsStep);
        this.workflow.addStep(getQuizStep);
        this.workflow.addStep(updateQuizStep);
    }

    public QuizDto getQuizDto() {
        return quizDto;
    }
}

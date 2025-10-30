package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.quiz.GetQuizByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.quiz.UpdateQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizQuestion;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.sagas.states.QuizSagaState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class UpdateQuizFunctionalitySagas extends WorkflowFunctionality {
    private QuizDto quiz;
    private QuizDto updatedQuizDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public UpdateQuizFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                        QuizFactory quizFactory,
                                        QuizDto quizDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(quizDto, quizFactory, unitOfWork);
    }

    public void buildWorkflow(QuizDto quizDto, QuizFactory quizFactory, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getQuizStep = new SagaSyncStep("getQuizStep", () -> {
            GetQuizByIdCommand getQuizByIdCommand = new GetQuizByIdCommand(unitOfWork, ServiceMapping.QUIZ.getServiceName(), quizDto.getAggregateId());
            getQuizByIdCommand.setSemanticLock(QuizSagaState.READ_QUIZ);
            QuizDto quiz = (QuizDto) commandGateway.send(getQuizByIdCommand);
            this.setQuiz(quiz);
        });

        getQuizStep.registerCompensation(() -> {
            Command command = new Command(unitOfWork, ServiceMapping.QUIZ.getServiceName(), quiz.getAggregateId());
            command.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
            commandGateway.send(command);
        }, unitOfWork);

        SagaSyncStep updateQuizStep = new SagaSyncStep("updateQuizStep", () -> {
            Set<QuizQuestion> quizQuestions = quizDto.getQuestionDtos().stream().map(QuizQuestion::new).collect(Collectors.toSet());
            UpdateQuizCommand updateQuizCommand = new UpdateQuizCommand(unitOfWork, ServiceMapping.QUIZ.getServiceName(), quizDto, quizQuestions);
            updatedQuizDto = (QuizDto) commandGateway.send(updateQuizCommand);
            this.setUpdatedQuizDto(updatedQuizDto);
        }, new ArrayList<>(Arrays.asList(getQuizStep)));

        workflow.addStep(getQuizStep);
        workflow.addStep(updateQuizStep);
    }

    public QuizDto getQuiz() {
        return quiz;
    }

    public void setQuiz(QuizDto quiz) {
        this.quiz = quiz;
    }

    public QuizDto getUpdatedQuizDto() {
        return updatedQuizDto;
    }

    public void setUpdatedQuizDto(QuizDto updatedQuizDto) {
        this.updatedQuizDto = updatedQuizDto;
    }
}
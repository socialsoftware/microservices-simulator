package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.quiz;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.quiz.GetQuizByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizQuestion;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.states.QuizSagaState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class UpdateQuizFunctionalitySagas extends WorkflowFunctionality {
    private QuizDto quiz;
    private QuizDto updatedQuizDto;
    private final QuizService quizService;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public UpdateQuizFunctionalitySagas(QuizService quizService, SagaUnitOfWorkService unitOfWorkService, QuizFactory quizFactory,
                                        QuizDto quizDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.quizService = quizService;
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(quizDto, quizFactory, unitOfWork);
    }

    public void buildWorkflow(QuizDto quizDto, QuizFactory quizFactory, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getQuizStep = new SagaSyncStep("getQuizStep", () -> {
//            QuizDto quiz = (QuizDto) quizService.getQuizById(quizDto.getAggregateId(), unitOfWork);
//            unitOfWorkService.registerSagaState(quiz.getAggregateId(), QuizSagaState.READ_QUIZ, unitOfWork);
            GetQuizByIdCommand getQuizByIdCommand = new GetQuizByIdCommand(unitOfWork, ServiceMapping.QUIZ.getServiceName(), quizDto.getAggregateId());
            getQuizByIdCommand.setSemanticLock(QuizSagaState.READ_QUIZ);
            QuizDto quiz = (QuizDto) commandGateway.send(getQuizByIdCommand);
            this.setQuiz(quiz);
        });
    
        getQuizStep.registerCompensation(() -> {
            unitOfWorkService.registerSagaState(quiz.getAggregateId(), GenericSagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);
    
        SagaSyncStep updateQuizStep = new SagaSyncStep("updateQuizStep", () -> {
            Set<QuizQuestion> quizQuestions = quizDto.getQuestionDtos().stream().map(QuizQuestion::new).collect(Collectors.toSet());
            QuizDto updatedQuizDto = quizService.updateQuiz(quizDto, quizQuestions, unitOfWork);
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
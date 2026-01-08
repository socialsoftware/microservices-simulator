package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.quiz;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.QuizExecution;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.service.ExecutionService;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos.SagaExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.states.ExecutionSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import java.util.ArrayList;
import java.util.Arrays;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.QuizQuestion;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos.SagaQuestionDto;
import java.util.Set;
import java.util.HashSet;

public class CreateQuizFunctionalitySagas extends WorkflowFunctionality {
    private QuizDto createdQuizDto;
    private final QuizService quizService;
    private final SagaUnitOfWorkService unitOfWorkService;
    private SagaExecutionDto executionDto;
    private QuizExecution execution;
    private final ExecutionService executionService;
    private final QuestionService questionService;


    public CreateQuizFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, QuizService quizService, ExecutionService executionService, QuestionService questionService, QuizDto quizDto) {
        this.quizService = quizService;
        this.unitOfWorkService = unitOfWorkService;
        this.executionService = executionService;
        this.questionService = questionService;
        this.buildWorkflow(quizDto, unitOfWork);
    }

    public void buildWorkflow(QuizDto quizDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getExecutionStep = new SagaSyncStep("getExecutionStep", () -> {
            Integer executionAggregateId = quizDto.getExecutionAggregateId();
            executionDto = (SagaExecutionDto) executionService.getExecutionById(executionAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(executionDto.getAggregateId(), ExecutionSagaState.READ_EXECUTION, unitOfWork);
            QuizExecution execution = new QuizExecution(executionDto);
            setExecution(execution);
        });

        getExecutionStep.registerCompensation(() -> {
            unitOfWorkService.registerSagaState(executionDto.getAggregateId(), GenericSagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);

        SagaSyncStep createQuizStep = new SagaSyncStep("createQuizStep", () -> {
            Set<QuizQuestion> questions = null;
            if (quizDto.getQuestionsAggregateIds() != null) {
                questions = new HashSet<>();
                for (Integer questionAggregateId : quizDto.getQuestionsAggregateIds()) {
                    SagaQuestionDto questionDto = (SagaQuestionDto) questionService.getQuestionById(questionAggregateId, unitOfWork);
                    questions.add(new QuizQuestion(questionDto));
                }
            }
            QuizDto createdQuizDto = quizService.createQuiz(getExecution(), quizDto, questions, unitOfWork);
            setCreatedQuizDto(createdQuizDto);
        }, new ArrayList<>(Arrays.asList(getExecutionStep)));

        workflow.addStep(getExecutionStep);
        workflow.addStep(createQuizStep);

    }

    public QuizExecution getExecution() {
        return execution;
    }

    public void setExecution(QuizExecution execution) {
        this.execution = execution;
    }

    public QuizDto getCreatedQuizDto() {
        return createdQuizDto;
    }

    public void setCreatedQuizDto(QuizDto createdQuizDto) {
        this.createdQuizDto = createdQuizDto;
    }
}

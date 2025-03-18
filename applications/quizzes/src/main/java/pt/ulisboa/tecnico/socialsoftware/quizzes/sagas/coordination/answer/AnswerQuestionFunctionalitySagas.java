package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.answer;

import java.util.ArrayList;
import java.util.Arrays;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuestionAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuizAnswerFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.service.QuizAnswerService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.dtos.SagaQuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.dtos.SagaQuizAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.states.QuestionSagaState;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.states.QuizAnswerSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class AnswerQuestionFunctionalitySagas extends WorkflowFunctionality {
    
    private SagaQuestionDto questionDto;
    private SagaQuizAnswerDto quizAnswer;
    private final QuizAnswerService quizAnswerService;
    private final QuestionService questionService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public AnswerQuestionFunctionalitySagas(QuizAnswerService quizAnswerService, QuestionService questionService, SagaUnitOfWorkService unitOfWorkService, QuizAnswerFactory quizAnswerFactory, 
                            Integer quizAggregateId, Integer userAggregateId, QuestionAnswerDto userQuestionAnswerDto, SagaUnitOfWork unitOfWork) {
        this.quizAnswerService = quizAnswerService;
        this.questionService = questionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(quizAggregateId, userAggregateId, userQuestionAnswerDto, quizAnswerFactory, unitOfWork);
    }

    public void buildWorkflow(Integer quizAggregateId, Integer userAggregateId, QuestionAnswerDto userQuestionAnswerDto, QuizAnswerFactory quizAnswerFactory, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getQuestionStep = new SagaSyncStep("getQuestionStep", () -> {
            SagaQuestionDto questionDto = (SagaQuestionDto) questionService.getQuestionById(userQuestionAnswerDto.getQuestionAggregateId(), unitOfWork);
            unitOfWorkService.registerSagaState(userQuestionAnswerDto.getQuestionAggregateId(), QuestionSagaState.READ_QUESTION, unitOfWork);
            this.setQuestionDto(questionDto);
        });

        getQuestionStep.registerCompensation(() -> {
            unitOfWorkService.registerSagaState(this.questionDto.getAggregateId(), GenericSagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);

        SagaSyncStep getQuizAnswerStep = new SagaSyncStep("getQuizAnswerStep", () -> {
            SagaQuizAnswerDto quizAnswer = (SagaQuizAnswerDto) quizAnswerService.getQuizAnswerDtoByQuizIdAndUserId(quizAggregateId, userAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(quizAggregateId, QuizAnswerSagaState.READ_QUIZ_ANSWER, unitOfWork);
            this.setQuizAnswer(quizAnswer);
        });

        getQuizAnswerStep.registerCompensation(() -> {
            unitOfWorkService.registerSagaState(this.quizAnswer.getAggregateId(), GenericSagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);

        SagaSyncStep answerQuestionStep = new SagaSyncStep("answerQuestionStep", () -> {
            quizAnswerService.answerQuestion(quizAggregateId, userAggregateId, userQuestionAnswerDto, this.getQuestionDto(), unitOfWork);
        }, new ArrayList<>(Arrays.asList(getQuestionStep, getQuizAnswerStep)));

        workflow.addStep(getQuestionStep);
        workflow.addStep(getQuizAnswerStep);
        workflow.addStep(answerQuestionStep);
    }
    

    public SagaQuestionDto getQuestionDto() {
        return questionDto;
    }

    public void setQuestionDto(SagaQuestionDto questionDto) {
        this.questionDto = questionDto;
    }

    public SagaQuizAnswerDto getQuizAnswer() {
        return quizAnswer;
    }

    public void setQuizAnswer(SagaQuizAnswerDto quizAnswer) {
        this.quizAnswer = quizAnswer;
    }
}
package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows;

import java.util.ArrayList;
import java.util.Arrays;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate.QuestionAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate.QuizAnswer;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate.QuizAnswerFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.service.QuizAnswerService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaQuestion;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaQuizAnswer;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class AnswerQuestionFunctionalitySagas extends WorkflowFunctionality {
    public enum State implements SagaState {
        ANSWER_QUESTION_READ_QUESTION {
            @Override
            public String getStateName() {
                return "ANSWER_QUESTION_READ_QUESTION";
            }
        },
        ANSWER_QUESTION_READ_QUIZ_ANSWER {
            @Override
            public String getStateName() {
                return "ANSWER_QUESTION_READ_QUIZ_ANSWER";
            }
        }
    }
    
    private QuestionDto questionDto;
    private QuizAnswer oldQuizAnswer;

    

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

        SyncStep getQuestionStep = new SyncStep("getQuestionStep", () -> {
            QuestionDto questionDto = questionService.getQuestionById(userQuestionAnswerDto.getQuestionAggregateId(), unitOfWork);
            SagaQuestion question = (SagaQuestion) unitOfWorkService.aggregateLoadAndRegisterRead(questionDto.getAggregateId(), unitOfWork);
            unitOfWorkService.registerSagaState(question, State.ANSWER_QUESTION_READ_QUESTION, unitOfWork);
            this.setQuestionDto(questionDto);
        });

        getQuestionStep.registerCompensation(() -> {
            QuestionDto questionDto = this.getQuestionDto();
            SagaQuestion question = (SagaQuestion) unitOfWorkService.aggregateLoadAndRegisterRead(questionDto.getAggregateId(), unitOfWork);
            unitOfWorkService.registerSagaState(question, GenericSagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);

        SyncStep getOldQuizAnswerStep = new SyncStep("getOldQuizAnswerStep", () -> {
            SagaQuizAnswer oldQuizAnswer = (SagaQuizAnswer) quizAnswerService.getQuizAnswerByQuizIdAndUserId(quizAggregateId, userAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(oldQuizAnswer, State.ANSWER_QUESTION_READ_QUIZ_ANSWER, unitOfWork);
            this.setOldQuizAnswer(oldQuizAnswer);
        });

        getOldQuizAnswerStep.registerCompensation(() -> {
            QuizAnswer newQuizAnswer = quizAnswerFactory.createQuizAnswerFromExisting(this.getOldQuizAnswer());
            unitOfWorkService.registerSagaState((SagaQuizAnswer) newQuizAnswer, GenericSagaState.NOT_IN_SAGA, unitOfWork);
            unitOfWork.registerChanged(newQuizAnswer);
        }, unitOfWork);

        SyncStep answerQuestionStep = new SyncStep("answerQuestionStep", () -> {
            quizAnswerService.answerQuestion(quizAggregateId, userAggregateId, userQuestionAnswerDto, this.getQuestionDto(), unitOfWork);
        }, new ArrayList<>(Arrays.asList(getQuestionStep, getOldQuizAnswerStep)));

        workflow.addStep(getQuestionStep);
        workflow.addStep(getOldQuizAnswerStep);
        workflow.addStep(answerQuestionStep);
    }

    @Override
    public void handleEvents() {

    }

    

    public QuestionDto getQuestionDto() {
        return questionDto;
    }

    public void setQuestionDto(QuestionDto questionDto) {
        this.questionDto = questionDto;
    }

    public QuizAnswer getOldQuizAnswer() {
        return oldQuizAnswer;
    }

    public void setOldQuizAnswer(QuizAnswer oldQuizAnswer) {
        this.oldQuizAnswer = oldQuizAnswer;
    }
}
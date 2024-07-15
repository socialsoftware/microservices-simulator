package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalities;

import java.util.ArrayList;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate.QuestionAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate.QuizAnswer;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate.QuizAnswerFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.service.QuizAnswerFunctionalitiesInterface;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.service.QuizAnswerService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaQuestion;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaQuiz;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaQuizAnswer;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.AnswerQuestionData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.ConcludeQuizData;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.data.StartQuizData;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

@Profile("sagas")
@Service
public class SagaQuizAnswerFunctionalities implements QuizAnswerFunctionalitiesInterface {
    @Autowired
    private QuizAnswerService quizAnswerService;
    @Autowired
    private QuestionService questionService;
    @Autowired
    private SagaUnitOfWorkService unitOfWorkService;
    @Autowired
    private QuizAnswerFactory quizAnswerFactory;
    @Autowired
    private QuizService quizService;

    public void answerQuestion(Integer quizAggregateId, Integer userAggregateId, QuestionAnswerDto userQuestionAnswerDto) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);

        AnswerQuestionData data = new AnswerQuestionData();
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, functionalityName, unitOfWork);

        SyncStep getQuestionStep = new SyncStep(() -> {
            QuestionDto questionDto = questionService.getQuestionById(userQuestionAnswerDto.getQuestionAggregateId(), unitOfWork);
            SagaQuestion question = (SagaQuestion) unitOfWorkService.aggregateLoadAndRegisterRead(questionDto.getAggregateId(), unitOfWork);
            unitOfWorkService.registerSagaState(question, SagaState.IN_SAGA, unitOfWork);
            data.setQuestionDto(questionDto);
        });

        getQuestionStep.registerCompensation(() -> {
            QuestionDto questionDto = data.getQuestionDto();
            SagaQuestion question = (SagaQuestion) unitOfWorkService.aggregateLoadAndRegisterRead(questionDto.getAggregateId(), unitOfWork);
            unitOfWorkService.registerSagaState(question, SagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);

        SyncStep getOldQuizAnswerStep = new SyncStep(() -> {
            SagaQuizAnswer oldQuizAnswer = (SagaQuizAnswer) quizAnswerService.getQuizAnswerByQuizIdAndUserId(quizAggregateId, userAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(oldQuizAnswer, SagaState.IN_SAGA, unitOfWork);
            data.setOldQuizAnswer(oldQuizAnswer);
        });

        getOldQuizAnswerStep.registerCompensation(() -> {
            QuizAnswer newQuizAnswer = quizAnswerFactory.createQuizAnswerFromExisting(data.getOldQuizAnswer());
            unitOfWorkService.registerSagaState((SagaQuizAnswer) newQuizAnswer, SagaState.NOT_IN_SAGA, unitOfWork);
            unitOfWork.registerChanged(newQuizAnswer);
        }, unitOfWork);

        SyncStep answerQuestionStep = new SyncStep(() -> {
            quizAnswerService.answerQuestion(quizAggregateId, userAggregateId, userQuestionAnswerDto, data.getQuestionDto(), unitOfWork);
        }, new ArrayList<>(Arrays.asList(getQuestionStep, getOldQuizAnswerStep)));

        workflow.addStep(getQuestionStep);
        workflow.addStep(getOldQuizAnswerStep);
        workflow.addStep(answerQuestionStep);

        workflow.execute(unitOfWork);
    }

    public void startQuiz(Integer quizAggregateId, Integer courseExecutionAggregateId, Integer userAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        StartQuizData data = new StartQuizData();
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, functionalityName, unitOfWork);
    
        SyncStep getQuizStep = new SyncStep(() -> {
            QuizDto quizDto = quizService.getQuizById(quizAggregateId, unitOfWork);
            SagaQuiz quiz = (SagaQuiz) unitOfWorkService.aggregateLoadAndRegisterRead(quizDto.getAggregateId(), unitOfWork);
            unitOfWorkService.registerSagaState(quiz, SagaState.IN_SAGA, unitOfWork);
            data.setQuizDto(quizDto);
        });
    
        getQuizStep.registerCompensation(() -> {
            SagaQuiz quiz = (SagaQuiz) unitOfWorkService.aggregateLoadAndRegisterRead(quizAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(quiz, SagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);
    
        SyncStep startQuizStep = new SyncStep(() -> {
            quizAnswerService.startQuiz(quizAggregateId, courseExecutionAggregateId, userAggregateId, unitOfWork);
        }, new ArrayList<>(Arrays.asList(getQuizStep)));
    
        startQuizStep.registerCompensation(() -> {
            QuizAnswer quizAnswer = quizAnswerService.getQuizAnswerByQuizIdAndUserId(quizAggregateId, userAggregateId, unitOfWork);
            unitOfWork.registerChanged(quizAnswer);
        }, unitOfWork);
    
        workflow.addStep(getQuizStep);
        workflow.addStep(startQuizStep);
    
        workflow.execute(unitOfWork);
    }

    public void concludeQuiz(Integer quizAggregateId, Integer userAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        ConcludeQuizData data = new ConcludeQuizData();
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, functionalityName, unitOfWork);
    
        SyncStep getQuizAnswerStep = new SyncStep(() -> {
            SagaQuizAnswer quizAnswer = (SagaQuizAnswer) quizAnswerService.getQuizAnswerByQuizIdAndUserId(quizAggregateId, userAggregateId, unitOfWork);
            unitOfWorkService.registerSagaState(quizAnswer, SagaState.IN_SAGA, unitOfWork);
            data.setQuizAnswer(quizAnswer);
        });
    
        getQuizAnswerStep.registerCompensation(() -> {
            SagaQuizAnswer quizAnswer = data.getQuizAnswer();
            quizAnswer.setCompleted(false);
            unitOfWorkService.registerSagaState(quizAnswer, SagaState.NOT_IN_SAGA, unitOfWork);
            unitOfWork.registerChanged(quizAnswer);
        }, unitOfWork);
    
        SyncStep concludeQuizStep = new SyncStep(() -> {
            quizAnswerService.concludeQuiz(quizAggregateId, userAggregateId, unitOfWork);
        }, new ArrayList<>(Arrays.asList(getQuizAnswerStep)));
    
        workflow.addStep(getQuizAnswerStep);
        workflow.addStep(concludeQuizStep);
    
        workflow.execute(unitOfWork);
    }
}
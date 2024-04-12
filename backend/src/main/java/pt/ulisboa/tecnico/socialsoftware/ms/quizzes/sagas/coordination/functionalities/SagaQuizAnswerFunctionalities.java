package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate.QuestionAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.service.QuizAnswerFunctionalitiesInterface;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.service.QuizAnswerService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.service.QuestionService;

@Profile("sagas")
@Service
public class SagaQuizAnswerFunctionalities implements QuizAnswerFunctionalitiesInterface {
    @Autowired
    private QuizAnswerService quizAnswerService;
    @Autowired
    private QuestionService questionService;
    @Autowired
    private SagaUnitOfWorkService unitOfWorkService;

    public void answerQuestion(Integer quizAggregateId, Integer userAggregateId, QuestionAnswerDto userQuestionAnswerDto) throws Exception {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        try {
            QuestionDto questionDto = questionService.getQuestionById(userQuestionAnswerDto.getQuestionAggregateId(), unitOfWork);
            //TODO check compensation
            quizAnswerService.answerQuestion(quizAggregateId, userAggregateId, userQuestionAnswerDto, questionDto, unitOfWork);
            unitOfWorkService.commit(unitOfWork);
        } catch (Exception ex) {
            unitOfWorkService.compensate(unitOfWork);
            throw new Exception("Error answering question", ex);
        }
    }

    public void startQuiz(Integer quizAggregateId, Integer courseExecutionAggregateId, Integer userAggregateId) throws Exception {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        try {
            quizAnswerService.startQuiz(quizAggregateId, courseExecutionAggregateId, userAggregateId, unitOfWork);
            //TODO check compensation
            unitOfWorkService.commit(unitOfWork);
        } catch (Exception ex) {
            unitOfWorkService.compensate(unitOfWork);
            throw new Exception("Error starting quiz", ex);
        }
    }

    public void concludeQuiz(Integer quizAggregateId, Integer courseExecutionAggregateId) throws Exception {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        try {
            quizAnswerService.concludeQuiz(quizAggregateId, courseExecutionAggregateId, unitOfWork);
            //TODO check compensation
            unitOfWorkService.commit(unitOfWork);
        } catch (Exception ex) {
            unitOfWorkService.compensate(unitOfWork);
            throw new Exception("Error concluding quiz", ex);
        }
    }
}
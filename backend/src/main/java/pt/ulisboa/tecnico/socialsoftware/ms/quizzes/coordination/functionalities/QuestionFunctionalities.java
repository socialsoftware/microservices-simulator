package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.functionalities;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.CreateQuestionFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.FindQuestionByAggregateIdFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.FindQuestionsByCourseFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.RemoveQuestionFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.UpdateQuestionFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.UpdateQuestionTopicsFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.course.service.CourseService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.CreateQuestionFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.FindQuestionByAggregateIdFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.FindQuestionsByCourseFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.RemoveQuestionFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.UpdateQuestionFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.UpdateQuestionTopicsFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;

@Service
public class QuestionFunctionalities {
    @Autowired(required = false)
    private SagaUnitOfWorkService sagaUnitOfWorkService;
    @Autowired(required = false)
    private CausalUnitOfWorkService causalUnitOfWorkService;
    @Autowired
    private QuestionService questionService;
    @Autowired
    private CourseService courseService;
    @Autowired
    private TopicService topicService;
    @Autowired
    private QuestionFactory questionFactory;

    @Autowired
    private Environment env;

    private String workflowType;

    @PostConstruct
    public void init() {
        // Determine the workflow type based on active profiles
        String[] activeProfiles = env.getActiveProfiles();
        if (Arrays.asList(activeProfiles).contains("sagas")) {
            workflowType = "sagas";
        } else if (Arrays.asList(activeProfiles).contains("tcc")) {
            workflowType = "tcc";
        } else {
            workflowType = "unknown"; // Default or fallback value
        }
    }

    public QuestionDto findQuestionByAggregateId(Integer aggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        if ("sagas".equals(workflowType)) {
            SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
            FindQuestionByAggregateIdFunctionalitySagas functionality = new FindQuestionByAggregateIdFunctionalitySagas(
                    questionService, sagaUnitOfWorkService, aggregateId, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
            return functionality.getQuestionDto();
        } else {
            CausalUnitOfWork unitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
            FindQuestionByAggregateIdFunctionalityTCC functionality = new FindQuestionByAggregateIdFunctionalityTCC(
                    questionService, causalUnitOfWorkService, aggregateId, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
            return functionality.getQuestionDto();
        }
    }

    public List<QuestionDto> findQuestionsByCourseAggregateId(Integer courseAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        if ("sagas".equals(workflowType)) {
            SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
            FindQuestionsByCourseFunctionalitySagas functionality = new FindQuestionsByCourseFunctionalitySagas(
                    questionService, sagaUnitOfWorkService, courseAggregateId, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
            return functionality.getQuestions();
        } else {
            CausalUnitOfWork unitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
            FindQuestionsByCourseFunctionalityTCC functionality = new FindQuestionsByCourseFunctionalityTCC(
                    questionService, causalUnitOfWorkService, courseAggregateId, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
            return functionality.getQuestions();
        }
    }

    public QuestionDto createQuestion(Integer courseAggregateId, QuestionDto questionDto) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        if ("sagas".equals(workflowType)) {
            SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
            CreateQuestionFunctionalitySagas functionality = new CreateQuestionFunctionalitySagas(
                    questionService, topicService, courseService, sagaUnitOfWorkService, courseAggregateId, questionDto, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
            return functionality.getCreatedQuestion();
        } else {
            CausalUnitOfWork unitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
            CreateQuestionFunctionalityTCC functionality = new CreateQuestionFunctionalityTCC(
                    questionService, topicService, courseService, causalUnitOfWorkService, courseAggregateId, questionDto, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
            return functionality.getCreatedQuestion();
        }
    }

    public void updateQuestion(QuestionDto questionDto) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        if ("sagas".equals(workflowType)) {
            SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
            UpdateQuestionFunctionalitySagas functionality = new UpdateQuestionFunctionalitySagas(
                    questionService, sagaUnitOfWorkService, questionFactory, questionDto, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
        } else {
            CausalUnitOfWork unitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
            UpdateQuestionFunctionalityTCC functionality = new UpdateQuestionFunctionalityTCC(
                    questionService, causalUnitOfWorkService, questionFactory, questionDto, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
        }
    }

    public void removeQuestion(Integer questionAggregateId) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        if ("sagas".equals(workflowType)) {
            SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
            RemoveQuestionFunctionalitySagas functionality = new RemoveQuestionFunctionalitySagas(
                    questionService, sagaUnitOfWorkService, questionAggregateId, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
        } else {
            CausalUnitOfWork unitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
            RemoveQuestionFunctionalityTCC functionality = new RemoveQuestionFunctionalityTCC(
                    questionService, causalUnitOfWorkService, questionAggregateId, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
        }
    }

    public void updateQuestionTopics(Integer courseAggregateId, List<Integer> topicIds) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        if ("sagas".equals(workflowType)) {
            SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
            UpdateQuestionTopicsFunctionalitySagas functionality = new UpdateQuestionTopicsFunctionalitySagas(
                    questionService, topicService, questionFactory, sagaUnitOfWorkService, courseAggregateId, topicIds, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
        } else {
            CausalUnitOfWork unitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
            UpdateQuestionTopicsFunctionalityTCC functionality = new UpdateQuestionTopicsFunctionalityTCC(
                    questionService, topicService, questionFactory, causalUnitOfWorkService, courseAggregateId, topicIds, unitOfWork);
            functionality.executeWorkflow(unitOfWork);
        }
    }

}

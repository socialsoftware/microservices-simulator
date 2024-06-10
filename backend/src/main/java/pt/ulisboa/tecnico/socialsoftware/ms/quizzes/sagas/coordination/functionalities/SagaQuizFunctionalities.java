package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.Quiz;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizCourseExecution;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizQuestion;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.service.QuizFunctionalitiesInterface;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.User;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

import static pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState.ACTIVE;
import static pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState.IN_SAGA;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Profile("sagas")
@Service
public class SagaQuizFunctionalities implements QuizFunctionalitiesInterface{
    @Autowired
    private SagaUnitOfWorkService unitOfWorkService;
    @Autowired
    private QuizService quizService;
    @Autowired
    private CourseExecutionService courseExecutionService;
    @Autowired
    private QuestionService questionService;
    @Autowired
    private QuizFactory quizFactory;

    public QuizDto createQuiz(Integer courseExecutionId, QuizDto quizDto) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);

        CreateQuizData data = new CreateQuizData();
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, functionalityName);

        SyncStep getCourseExecutionStep = new SyncStep(() -> {
            QuizCourseExecution quizCourseExecution = new QuizCourseExecution(courseExecutionService.getCourseExecutionById(courseExecutionId, unitOfWork));
            data.setQuizCourseExecution(quizCourseExecution);
        });

        SyncStep getQuestionsStep = new SyncStep(() -> {
            Set<QuestionDto> questions = quizDto.getQuestionDtos().stream()
                    .map(qq -> questionService.getQuestionById(qq.getAggregateId(), unitOfWork))
                    .collect(Collectors.toSet());
            data.setQuestions(questions);
        });

        SyncStep createQuizStep = new SyncStep(() -> {
            QuizDto createdQuizDto = quizService.createQuiz(data.getQuizCourseExecution(), data.getQuestions(), quizDto, unitOfWork);
            data.setCreatedQuizDto(createdQuizDto);
        });

        createQuizStep.registerCompensation(() -> {
            Quiz quiz = (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(data.getCreatedQuizDto().getAggregateId(), unitOfWork);
            quiz.remove();
            quiz.setState(AggregateState.DELETED);
            unitOfWork.registerChanged(quiz);
        }, unitOfWork);

        workflow.addStep(getCourseExecutionStep);
        workflow.addStep(getQuestionsStep);
        workflow.addStep(createQuizStep);

        workflow.execute();

        return data.getCreatedQuizDto();
    }

    public QuizDto findQuiz(Integer quizAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        FindQuizData data = new FindQuizData();
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, functionalityName);
    
        SyncStep findQuizStep = new SyncStep(() -> {
            QuizDto quizDto = quizService.getQuizById(quizAggregateId, unitOfWork);
            data.setQuizDto(quizDto);
        });
    
        workflow.addStep(findQuizStep);
        workflow.execute();
    
        return data.getQuizDto();
    }

    public List<QuizDto> getAvailableQuizzes(Integer userAggregateId, Integer courseExecutionAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        GetAvailableQuizzesData data = new GetAvailableQuizzesData();
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, functionalityName);
    
        SyncStep getAvailableQuizzesStep = new SyncStep(() -> {
            List<QuizDto> availableQuizzes = quizService.getAvailableQuizzes(courseExecutionAggregateId, unitOfWork);
            data.setAvailableQuizzes(availableQuizzes);
        });
    
        workflow.addStep(getAvailableQuizzesStep);
        workflow.execute();
    
        return data.getAvailableQuizzes();
    }

    public QuizDto updateQuiz(QuizDto quizDto) throws Exception {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
    
        UpdateQuizData data = new UpdateQuizData();
        SagaWorkflow workflow = new SagaWorkflow(data, unitOfWorkService, functionalityName);
    
        SyncStep getOldQuizStep = new SyncStep(() -> {
            Quiz oldQuiz = (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(quizDto.getAggregateId(), unitOfWork);
            oldQuiz.setState(AggregateState.IN_SAGA);
            data.setOldQuiz(oldQuiz);
        });
    
        getOldQuizStep.registerCompensation(() -> {
            Quiz newQuiz = quizFactory.createQuizFromExisting(data.getOldQuiz());
            unitOfWork.registerChanged(newQuiz);
            newQuiz.setState(AggregateState.ACTIVE);
        }, unitOfWork);
    
        SyncStep updateQuizStep = new SyncStep(() -> {
            Set<QuizQuestion> quizQuestions = quizDto.getQuestionDtos().stream().map(QuizQuestion::new).collect(Collectors.toSet());
            QuizDto updatedQuizDto = quizService.updateQuiz(quizDto, quizQuestions, unitOfWork);
            data.setUpdatedQuizDto(updatedQuizDto);
        });
    
        workflow.addStep(getOldQuizStep);
        workflow.addStep(updateQuizStep);
    
        workflow.execute();
    
        return data.getUpdatedQuizDto();
    }

}

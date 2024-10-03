package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizCourseExecution;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.dtos.SagaCourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.states.CourseExecutionSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.states.QuestionSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class CreateQuizFunctionalitySagas extends WorkflowFunctionality {
    private QuizCourseExecution quizCourseExecution;
    private Set<QuestionDto> questions;
    private QuizDto createdQuizDto;
    private SagaCourseExecutionDto courseExecutionDto;

    

    private final CourseExecutionService courseExecutionService;
    private final QuizService quizService;
    private final QuestionService questionService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public CreateQuizFunctionalitySagas(CourseExecutionService courseExecutionService, QuizService quizService, QuestionService questionService, SagaUnitOfWorkService unitOfWorkService,  
                            Integer courseExecutionId, QuizDto quizDto, SagaUnitOfWork unitOfWork) {
        this.courseExecutionService = courseExecutionService;
        this.quizService = quizService;
        this.questionService = questionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(courseExecutionId, quizDto, unitOfWork);
    }

    public void buildWorkflow(Integer courseExecutionId, QuizDto quizDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getCourseExecutionStep = new SagaSyncStep("getCourseExecutionStep", () -> {
            this.courseExecutionDto = (SagaCourseExecutionDto) courseExecutionService.getCourseExecutionById(courseExecutionId, unitOfWork);
            unitOfWorkService.registerSagaState(courseExecutionDto.getAggregateId(), CourseExecutionSagaState.READ_COURSE, unitOfWork);
            QuizCourseExecution quizCourseExecution = new QuizCourseExecution(courseExecutionDto);
            this.setQuizCourseExecution(quizCourseExecution);
        });

        getCourseExecutionStep.registerCompensation(() -> {
            unitOfWorkService.registerSagaState(courseExecutionDto.getAggregateId(), GenericSagaState.NOT_IN_SAGA, unitOfWork);
        }, unitOfWork);

        SagaSyncStep getQuestionsStep = new SagaSyncStep("getQuestionsStep", () -> {
            Set<QuestionDto> questions = quizDto.getQuestionDtos().stream()
                .map(questionDto -> {
                    QuestionDto question = questionService.getQuestionById(questionDto.getAggregateId(), unitOfWork);
                    unitOfWorkService.registerSagaState(question.getAggregateId(), QuestionSagaState.READ_QUESTION, unitOfWork);
                    return question;
                })
                .collect(Collectors.toSet());
            this.setQuestions(questions);
        });

        getQuestionsStep.registerCompensation(() -> {
            quizDto.getQuestionDtos().forEach(questionDto -> {
                unitOfWorkService.registerSagaState(questionDto.getAggregateId(), GenericSagaState.NOT_IN_SAGA, unitOfWork);
            });
        }, unitOfWork);

        SagaSyncStep createQuizStep = new SagaSyncStep("createQuizStep", () -> {
            QuizDto createdQuizDto = quizService.createQuiz(this.getQuizCourseExecution(), this.getQuestions(), quizDto, unitOfWork);
            this.setCreatedQuizDto(createdQuizDto);
        }, new ArrayList<>(Arrays.asList(getCourseExecutionStep, getQuestionsStep)));

        workflow.addStep(getCourseExecutionStep);
        workflow.addStep(getQuestionsStep);
        workflow.addStep(createQuizStep);
    }

    @Override
    public void handleEvents() {

    }

    public QuizCourseExecution getQuizCourseExecution() {
        return quizCourseExecution;
    }

    public void setQuizCourseExecution(QuizCourseExecution quizCourseExecution) {
        this.quizCourseExecution = quizCourseExecution;
    }

    public Set<QuestionDto> getQuestions() {
        return questions;
    }

    public void setQuestions(Set<QuestionDto> questions) {
        this.questions = questions;
    }

    public QuizDto getCreatedQuizDto() {
        return createdQuizDto;
    }

    public void setCreatedQuizDto(QuizDto createdQuizDto) {
        this.createdQuizDto = createdQuizDto;
    }
}
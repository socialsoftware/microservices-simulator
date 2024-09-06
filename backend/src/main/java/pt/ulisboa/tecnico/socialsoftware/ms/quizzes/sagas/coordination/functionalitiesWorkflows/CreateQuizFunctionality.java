package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.Quiz;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizCourseExecution;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class CreateQuizFunctionality extends WorkflowFunctionality {
    private QuizCourseExecution quizCourseExecution;
    private Set<QuestionDto> questions;
    private QuizDto createdQuizDto;

    private SagaWorkflow workflow;

    private final CourseExecutionService courseExecutionService;
    private final QuizService quizService;
    private final QuestionService questionService;
    private final SagaUnitOfWorkService unitOfWorkService;

    public CreateQuizFunctionality(CourseExecutionService courseExecutionService, QuizService quizService, QuestionService questionService, SagaUnitOfWorkService unitOfWorkService,  
                            Integer courseExecutionId, QuizDto quizDto, SagaUnitOfWork unitOfWork) {
        this.courseExecutionService = courseExecutionService;
        this.quizService = quizService;
        this.questionService = questionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(courseExecutionId, quizDto, unitOfWork);
    }

    public void buildWorkflow(Integer courseExecutionId, QuizDto quizDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep getCourseExecutionStep = new SyncStep("getCourseExecutionStep", () -> {
            QuizCourseExecution quizCourseExecution = new QuizCourseExecution(courseExecutionService.getCourseExecutionById(courseExecutionId, unitOfWork));
            this.setQuizCourseExecution(quizCourseExecution);
        });

        SyncStep getQuestionsStep = new SyncStep("getQuestionsStep", () -> {
            Set<QuestionDto> questions = quizDto.getQuestionDtos().stream()
                    .map(qq -> questionService.getQuestionById(qq.getAggregateId(), unitOfWork))
                    .collect(Collectors.toSet());
            this.setQuestions(questions);
        });

        SyncStep createQuizStep = new SyncStep("createQuizStep", () -> {
            QuizDto createdQuizDto = quizService.createQuiz(this.getQuizCourseExecution(), this.getQuestions(), quizDto, unitOfWork);
            this.setCreatedQuizDto(createdQuizDto);
        }, new ArrayList<>(Arrays.asList(getCourseExecutionStep, getQuestionsStep)));

        createQuizStep.registerCompensation(() -> {
            Quiz quiz = (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(this.getCreatedQuizDto().getAggregateId(), unitOfWork);
            quiz.remove();
            unitOfWork.registerChanged(quiz);
        }, unitOfWork);

        workflow.addStep(getCourseExecutionStep);
        workflow.addStep(getQuestionsStep);
        workflow.addStep(createQuizStep);
    }

    @Override
    public void handleEvents() {

    }

    public void executeWorkflow(SagaUnitOfWork unitOfWork) {
        workflow.execute(unitOfWork);
    }

    public void executeStepByName(String stepName, SagaUnitOfWork unitOfWork) {
        workflow.executeStepByName(stepName, unitOfWork);
    }

    public void executeUntilStep(String stepName, SagaUnitOfWork unitOfWork) {
        workflow.executeUntilStep(stepName, unitOfWork);
    }

    public void resumeWorkflow(SagaUnitOfWork unitOfWork) {
        workflow.resume(unitOfWork);
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
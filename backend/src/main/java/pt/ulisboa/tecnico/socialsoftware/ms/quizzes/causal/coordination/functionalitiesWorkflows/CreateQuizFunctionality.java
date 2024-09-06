package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.functionalitiesWorkflows;

import java.util.Set;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizCourseExecution;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.service.QuizService;

public class CreateQuizFunctionality extends WorkflowFunctionality {
    private QuizCourseExecution quizCourseExecution;
    private Set<QuestionDto> questions;
    private QuizDto createdQuizDto;

    private CausalWorkflow workflow;

    private final CourseExecutionService courseExecutionService;
    private final QuizService quizService;
    private final QuestionService questionService;
    private final CausalUnitOfWorkService unitOfWorkService;

    public CreateQuizFunctionality(CourseExecutionService courseExecutionService, QuizService quizService, QuestionService questionService, CausalUnitOfWorkService unitOfWorkService,  
                            Integer courseExecutionId, QuizDto quizDto, CausalUnitOfWork unitOfWork) {
        this.courseExecutionService = courseExecutionService;
        this.quizService = quizService;
        this.questionService = questionService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(courseExecutionId, quizDto, unitOfWork);
    }

    public void buildWorkflow(Integer courseExecutionId, QuizDto quizDto, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
            QuizCourseExecution quizCourseExecution = new QuizCourseExecution(courseExecutionService.getCourseExecutionById(courseExecutionId, unitOfWork));

            Set<QuestionDto> questions = quizDto.getQuestionDtos().stream()
                    .map(qq -> questionService.getQuestionById(qq.getAggregateId(), unitOfWork))
                    .collect(Collectors.toSet());

            this.createdQuizDto = quizService.createQuiz(quizCourseExecution, questions, quizDto, unitOfWork);
        });
    
        workflow.addStep(step);
    }

    @Override
    public void handleEvents() {

    }

    public void executeWorkflow(CausalUnitOfWork unitOfWork) {
        workflow.execute(unitOfWork);
    }

    public void executeStepByName(String stepName, CausalUnitOfWork unitOfWork) {
        workflow.executeStepByName(stepName, unitOfWork);
    }

    public void executeUntilStep(String stepName, CausalUnitOfWork unitOfWork) {
        workflow.executeUntilStep(stepName, unitOfWork);
    }

    public void resumeWorkflow(CausalUnitOfWork unitOfWork) {
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
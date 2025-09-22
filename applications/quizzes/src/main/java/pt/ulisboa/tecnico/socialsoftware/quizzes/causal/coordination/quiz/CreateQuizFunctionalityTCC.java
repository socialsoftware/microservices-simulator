package pt.ulisboa.tecnico.socialsoftware.quizzes.causal.coordination.quiz;

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.workflow.CausalWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.SyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.courseExecution.GetCourseExecutionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.question.GetQuestionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.quiz.CreateQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizCourseExecution;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;

import java.util.Set;
import java.util.stream.Collectors;

public class CreateQuizFunctionalityTCC extends WorkflowFunctionality {
    private QuizCourseExecution quizCourseExecution;
    private Set<QuestionDto> questions;
    private QuizDto createdQuizDto;
    private final CausalUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public CreateQuizFunctionalityTCC(CausalUnitOfWorkService unitOfWorkService,
            Integer courseExecutionId, QuizDto quizDto, CausalUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(courseExecutionId, quizDto, unitOfWork);
    }

    public void buildWorkflow(Integer courseExecutionId, QuizDto quizDto, CausalUnitOfWork unitOfWork) {
        this.workflow = new CausalWorkflow(this, unitOfWorkService, unitOfWork);

        SyncStep step = new SyncStep(() -> {
            // QuizCourseExecution quizCourseExecution = new
            // QuizCourseExecution(courseExecutionService.getCourseExecutionById(courseExecutionId,
            // unitOfWork));
            GetCourseExecutionByIdCommand GetCourseExecutionByIdCommand = new GetCourseExecutionByIdCommand(unitOfWork,
                    ServiceMapping.COURSE_EXECUTION.getServiceName(), courseExecutionId);
            CourseExecutionDto courseExecutionDto = (CourseExecutionDto) commandGateway
                    .send(GetCourseExecutionByIdCommand);
            QuizCourseExecution quizCourseExecution = new QuizCourseExecution(courseExecutionDto);

            // Set<QuestionDto> questions = quizDto.getQuestionDtos().stream()
            // .map(qq -> questionService.getQuestionById(qq.getAggregateId(), unitOfWork))
            // .collect(Collectors.toSet());
            Set<QuestionDto> questions = quizDto.getQuestionDtos().stream()
                    .map(qq -> (QuestionDto) commandGateway.send(new GetQuestionByIdCommand(unitOfWork,
                            ServiceMapping.QUESTION.getServiceName(), qq.getAggregateId())))
                    .collect(Collectors.toSet());

            // this.createdQuizDto = quizService.createQuiz(quizCourseExecution, questions,
            // quizDto, unitOfWork);
            CreateQuizCommand CreateQuizCommand = new CreateQuizCommand(unitOfWork,
                    ServiceMapping.QUIZ.getServiceName(), quizCourseExecution, questions, quizDto);
            this.createdQuizDto = (QuizDto) commandGateway.send(CreateQuizCommand);
        });

        workflow.addStep(step);
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
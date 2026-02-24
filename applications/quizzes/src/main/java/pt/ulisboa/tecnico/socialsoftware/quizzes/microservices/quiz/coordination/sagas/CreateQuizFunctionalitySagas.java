package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.courseExecution.GetCourseExecutionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.question.GetQuestionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.quiz.CreateQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.sagas.states.CourseExecutionSagaState;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.sagas.states.QuestionSagaState;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizCourseExecution;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class CreateQuizFunctionalitySagas extends WorkflowFunctionality {
    private QuizCourseExecution quizCourseExecution;
    private Set<QuestionDto> questions;
    private QuizDto createdQuizDto;
    private CourseExecutionDto courseExecutionDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public CreateQuizFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                        Integer courseExecutionId, QuizDto quizDto, SagaUnitOfWork unitOfWork, CommandGateway CommandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = CommandGateway;
        this.buildWorkflow(courseExecutionId, quizDto, unitOfWork);
    }

    public void buildWorkflow(Integer courseExecutionId, QuizDto quizDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getCourseExecutionStep = new SagaStep("getCourseExecutionStep", () -> {
            GetCourseExecutionByIdCommand getCourseExecutionByIdCommand = new GetCourseExecutionByIdCommand(unitOfWork, ServiceMapping.COURSE_EXECUTION.getServiceName(), courseExecutionId);
            getCourseExecutionByIdCommand.setSemanticLock(CourseExecutionSagaState.READ_COURSE);
            this.courseExecutionDto = (CourseExecutionDto) commandGateway.send(getCourseExecutionByIdCommand);

            QuizCourseExecution quizCourseExecution = new QuizCourseExecution(courseExecutionDto);
            this.setQuizCourseExecution(quizCourseExecution);
        });

        getCourseExecutionStep.registerCompensation(() -> {
            Command command = new Command(unitOfWork, ServiceMapping.COURSE_EXECUTION.getServiceName(), courseExecutionId);
            command.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
            commandGateway.send(command);
        }, unitOfWork);

        SagaStep getQuestionsStep = new SagaStep("getQuestionsStep", () -> { // TODO
            Set<QuestionDto> questions = quizDto.getQuestionDtos().stream()
                    .map(questionDto -> {
                        GetQuestionByIdCommand getQuestionByIdCommand = new GetQuestionByIdCommand(unitOfWork, ServiceMapping.QUESTION.getServiceName(), questionDto.getAggregateId());
                        getQuestionByIdCommand.setSemanticLock(QuestionSagaState.READ_QUESTION);
                        return (QuestionDto) commandGateway.send(getQuestionByIdCommand);
                    })
                    .collect(Collectors.toSet());
            this.setQuestions(questions);
        });

        getQuestionsStep.registerCompensation(() -> {
            quizDto.getQuestionDtos().forEach(questionDto -> {
                Command command = new Command(unitOfWork, ServiceMapping.QUESTION.getServiceName(), questionDto.getAggregateId());
                command.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
                commandGateway.send(command);
            });
        }, unitOfWork);

        SagaStep createQuizStep = new SagaStep("createQuizStep", () -> {
            CreateQuizCommand createQuizCommand = new CreateQuizCommand(unitOfWork, ServiceMapping.QUIZ.getServiceName(), this.getQuizCourseExecution(), this.getQuestions(), quizDto);
            QuizDto createdQuizDto = (QuizDto) commandGateway.send(createQuizCommand);
            this.setCreatedQuizDto(createdQuizDto);
        }, new ArrayList<>(Arrays.asList(getCourseExecutionStep, getQuestionsStep)));

        workflow.addStep(getCourseExecutionStep);
        workflow.addStep(getQuestionsStep);
        workflow.addStep(createQuizStep);
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
package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.quiz;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.courseExecution.GetCourseExecutionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.question.GetQuestionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.quiz.CreateQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizCourseExecution;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.states.CourseExecutionSagaState;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.states.QuestionSagaState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class CreateQuizFunctionalitySagas extends WorkflowFunctionality {
    private QuizCourseExecution quizCourseExecution;
    private Set<QuestionDto> questions;
    private QuizDto createdQuizDto;
    private CourseExecutionDto courseExecutionDto;
    private final CourseExecutionService courseExecutionService;
    private final QuizService quizService;
    private final QuestionService questionService;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public CreateQuizFunctionalitySagas(CourseExecutionService courseExecutionService, QuizService quizService, QuestionService questionService, SagaUnitOfWorkService unitOfWorkService,  
                            Integer courseExecutionId, QuizDto quizDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.courseExecutionService = courseExecutionService;
        this.quizService = quizService;
        this.questionService = questionService;
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(courseExecutionId, quizDto, unitOfWork);
    }

    public void buildWorkflow(Integer courseExecutionId, QuizDto quizDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getCourseExecutionStep = new SagaSyncStep("getCourseExecutionStep", () -> {
//            this.courseExecutionDto = (SagaCourseExecutionDto) courseExecutionService.getCourseExecutionById(courseExecutionId, unitOfWork);
//            unitOfWorkService.registerSagaState(courseExecutionDto.getAggregateId(), CourseExecutionSagaState.READ_COURSE, unitOfWork);
            GetCourseExecutionByIdCommand getCourseExecutionByIdCommand = new GetCourseExecutionByIdCommand(unitOfWork, ServiceMapping.COURSE_EXECUTION.getServiceName(), courseExecutionId);
            getCourseExecutionByIdCommand.setSemanticLock(CourseExecutionSagaState.READ_COURSE);
            this.courseExecutionDto = (CourseExecutionDto) commandGateway.send(getCourseExecutionByIdCommand);

            QuizCourseExecution quizCourseExecution = new QuizCourseExecution(courseExecutionDto);
            this.setQuizCourseExecution(quizCourseExecution);
        });

        getCourseExecutionStep.registerCompensation(() -> {
//            unitOfWorkService.registerSagaState(courseExecutionDto.getAggregateId(), GenericSagaState.NOT_IN_SAGA, unitOfWork);
            Command command = new Command(unitOfWork, ServiceMapping.COURSE_EXECUTION.getServiceName(), courseExecutionId);
            command.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
            commandGateway.send(command);
        }, unitOfWork);

        SagaSyncStep getQuestionsStep = new SagaSyncStep("getQuestionsStep", () -> {
            Set<QuestionDto> questions = quizDto.getQuestionDtos().stream()
                .map(questionDto -> {
//                    QuestionDto question = questionService.getQuestionById(questionDto.getAggregateId(), unitOfWork);
//                    unitOfWorkService.registerSagaState(question.getAggregateId(), QuestionSagaState.READ_QUESTION, unitOfWork);
                    GetQuestionByIdCommand getQuestionByIdCommand = new GetQuestionByIdCommand(unitOfWork, ServiceMapping.QUESTION.getServiceName(), questionDto.getAggregateId());
                    getQuestionByIdCommand.setSemanticLock(QuestionSagaState.READ_QUESTION);
                    QuestionDto question = (QuestionDto) commandGateway.send(getQuestionByIdCommand);
                    return question;
                })
                .collect(Collectors.toSet());
            this.setQuestions(questions);
        });

        getQuestionsStep.registerCompensation(() -> {
            quizDto.getQuestionDtos().forEach(questionDto -> {
//                unitOfWorkService.registerSagaState(questionDto.getAggregateId(), GenericSagaState.NOT_IN_SAGA, unitOfWork);
                Command command = new Command(unitOfWork, ServiceMapping.QUESTION.getServiceName(), questionDto.getAggregateId());
                command.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
                commandGateway.send(command);
            });
        }, unitOfWork);

        SagaSyncStep createQuizStep = new SagaSyncStep("createQuizStep", () -> {
//            QuizDto createdQuizDto = quizService.createQuiz(this.getQuizCourseExecution(), this.getQuestions(), quizDto, unitOfWork);
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